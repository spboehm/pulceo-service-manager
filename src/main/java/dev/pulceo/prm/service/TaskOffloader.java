package dev.pulceo.prm.service;

import dev.pulceo.prm.api.PnaApi;
import dev.pulceo.prm.api.PrmApi;
import dev.pulceo.prm.api.dto.task.CreateNewTaskOnPnaDTO;
import dev.pulceo.prm.api.dto.task.CreateNewTaskOnPnaResponseDTO;
import dev.pulceo.prm.api.exception.PnaApiException;
import dev.pulceo.prm.api.exception.PrmApiException;
import dev.pulceo.prm.dto.task.UpdateTaskFromPNADTO;
import dev.pulceo.prm.exception.TaskServiceException;
import dev.pulceo.prm.model.event.EventType;
import dev.pulceo.prm.model.event.PulceoEvent;
import dev.pulceo.prm.model.message.TaskMessage;
import dev.pulceo.prm.model.message.TaskStatusLogMessage;
import dev.pulceo.prm.model.task.Task;
import dev.pulceo.prm.model.task.TaskScheduling;
import dev.pulceo.prm.model.task.TaskStatus;
import dev.pulceo.prm.model.task.TaskStatusLog;
import dev.pulceo.prm.repository.TaskRepository;
import dev.pulceo.prm.repository.TaskSchedulingRepository;
import dev.pulceo.prm.repository.TaskStatusLogRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class TaskOffloader {

    private final Logger logger = LoggerFactory.getLogger(TaskOffloader.class);

    private final TaskRepository taskRepository;

    private final TaskSchedulingRepository taskSchedulingRepository;

    private final TaskStatusLogRepository taskStatusLogRepository;

    private final PublishSubscribeChannel taskServiceChannel;

    private final PrmApi prmApi;
    private final PnaApi pnaApi;

    private final EventHandler eventHandler;

    @Value("${psm.uuid}")
    private String PSM_UUID;

    public TaskOffloader(TaskRepository taskRepository, TaskSchedulingRepository taskSchedulingRepository, TaskStatusLogRepository taskStatusLogRepository, PublishSubscribeChannel taskServiceChannel, PrmApi prmApi, EventHandler eventHandler, PnaApi pnaApi) {
        this.taskRepository = taskRepository;
        this.taskSchedulingRepository = taskSchedulingRepository;
        this.taskStatusLogRepository = taskStatusLogRepository;
        this.taskServiceChannel = taskServiceChannel;
        this.prmApi = prmApi;
        this.eventHandler = eventHandler;
        this.pnaApi = pnaApi;
    }

    public void updateTaskFromPna(String pnaUUID, UpdateTaskFromPNADTO updateTaskFromPNADTO) throws TaskServiceException {
        logger.info("Updating task, received from PNA with payload %s".formatted(updateTaskFromPNADTO.toString()));
        if (updateTaskFromPNADTO.getNewTaskStatus() == TaskStatus.RUNNING || updateTaskFromPNADTO.getNewTaskStatus() == TaskStatus.COMPLETED) {
            // get task
            Optional<Task> taskOptional = this.taskRepository.findByUuid(UUID.fromString(updateTaskFromPNADTO.getGlobalTaskUUID()));
            if (taskOptional.isEmpty()) {
                logger.warn("Task with id %s not found".formatted(updateTaskFromPNADTO.getGlobalTaskUUID()));
                return;
            }
            Task task = taskOptional.get();

            // retrieve task scheduling
            Optional<TaskScheduling> taskScheduling = this.taskSchedulingRepository.findWithStatusLogsByUuid(task.getTaskScheduling().getUuid());
            if (taskScheduling.isEmpty()) {
                logger.warn("Task scheduling with id %s not found".formatted(task.getTaskScheduling().getUuid()));
                throw new TaskServiceException("Associated task scheduling not found");
            }
            TaskStatus previousTaskStatus = taskScheduling.get().getStatus();
            logger.debug("Previous task status is %s".formatted(previousTaskStatus));
            logger.debug("New task status is %s".formatted(updateTaskFromPNADTO.getNewTaskStatus()));

            TaskScheduling taskSchedulingToBeUpdated = taskScheduling.get();
            String stateOfTaskScheduling = taskSchedulingToBeUpdated.toString();
            taskSchedulingToBeUpdated.setStatus(updateTaskFromPNADTO.getNewTaskStatus());
            // create new TaskStatusLog
            try {
                String globalId = this.prmApi.resolvePnaUuidToGlobalId(pnaUUID);
                this.taskSchedulingRepository.save(taskSchedulingToBeUpdated);
                TaskStatusLog savedTaskStatusLog = this.taskStatusLogRepository.save(this.logStatusChange(previousTaskStatus, stateOfTaskScheduling, taskSchedulingToBeUpdated, task, updateTaskFromPNADTO.getModifiedOn(), globalId));
                // publish event to PMS via MQTT
                issueEventToPMS(EventType.fromTaskStatus(updateTaskFromPNADTO.getNewTaskStatus()), savedTaskStatusLog);
                // publish task status log to pms via MQTT
                issueTaskStatusLogToPMS(savedTaskStatusLog, taskSchedulingToBeUpdated);
                // issue to user
                // TODO: handle case running
                if (updateTaskFromPNADTO.getNewTaskStatus() == TaskStatus.COMPLETED) {
                    issueCompletedTaskToUser(task, TaskStatus.COMPLETED);
                }
                // TODO: broadcast to users
            } catch (PrmApiException | InterruptedException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                logger.error("Error while updating task status %s".formatted(e.getMessage()));
                throw new TaskServiceException("Error while updating task status %s".formatted(e.getMessage()));
            }
        } else {
            this.logger.warn("Unsupported task status, received status %s".formatted(updateTaskFromPNADTO.getNewTaskStatus()));
        }
    }

    @Transactional
    @Async
    public void offloadScheduledTasks(String taskSchedulingId) throws InterruptedException, PnaApiException, TaskServiceException {
        logger.info("Try to offload task with id %s".formatted(taskSchedulingId));
        // TODO: query from DB with UUID
        Optional<TaskScheduling> taskSchedulingOptional = this.taskSchedulingRepository.findWithTaskAndStatusLogsByUuid(UUID.fromString(taskSchedulingId));

        if (taskSchedulingOptional.isEmpty()) {
            logger.warn("TaskScheduling not found");
            return;
        }
        // retrieve offloaded task
        TaskScheduling taskSchedulingToBeOffloaded = taskSchedulingOptional.get();
        String oldTaskStatus = taskSchedulingToBeOffloaded.toString();
        logger.debug("Try to schedule task scheduling with old state: {}, status: {}, and global task UUID: {}", oldTaskStatus, taskSchedulingToBeOffloaded.getStatus(), taskSchedulingToBeOffloaded.getGlobalTaskUUID());

        // check if task has not been scheduled yet
        logger.debug("Offloaded has status %s and global task UUID %s".formatted(taskSchedulingToBeOffloaded.getStatus(), taskSchedulingToBeOffloaded.getGlobalTaskUUID()));
        if (taskSchedulingToBeOffloaded.getStatus() == TaskStatus.SCHEDULED) {
            logger.info("TaskScheduling to be offloaded has payload %s".formatted(taskSchedulingToBeOffloaded.toString()));
            // offload to corresponding PNA, blocking operation
            // TODO: exception handling properly?
            CreateNewTaskOnPnaResponseDTO createNewTaskOnPnaResponseDTO = offloadToPNA(taskSchedulingToBeOffloaded.getTask().getUuid().toString(), taskSchedulingToBeOffloaded);
            // global task UUID already set
            taskSchedulingToBeOffloaded.setGlobalTaskUUID(createNewTaskOnPnaResponseDTO.getGlobalTaskUUID());
            taskSchedulingToBeOffloaded.setRemoteTaskUUID(createNewTaskOnPnaResponseDTO.getRemoteTaskUUID().toString());
            taskSchedulingToBeOffloaded.setRemoteNodeUUID(createNewTaskOnPnaResponseDTO.getRemoteNodeUUID().toString());
            taskSchedulingToBeOffloaded.setStatus(TaskStatus.OFFLOADED);
            // persist task scheduling changes to DB,
            this.taskSchedulingRepository.save(taskSchedulingToBeOffloaded);
            // persist task scheduling logs
            Optional<Task> taskOptional = this.taskRepository.findByUuid(taskSchedulingToBeOffloaded.getTask().getUuid());
            // create task status log
            TaskStatusLog savedTaskStatusLog = this.taskStatusLogRepository.save(this.logStatusChange(TaskStatus.SCHEDULED, oldTaskStatus, taskSchedulingToBeOffloaded, taskOptional.get()));
            // publish event to PMS via MQTT
            issueEventToPMS(EventType.fromTaskStatus(taskSchedulingToBeOffloaded.getStatus()), savedTaskStatusLog);
            // publish task status log to pms via MQTT
            issueTaskStatusLogToPMS(savedTaskStatusLog, taskSchedulingToBeOffloaded);
            logger.info("Successfully offloaded task with id %s".formatted(taskSchedulingId));
        } else {
            logger.warn("Task with status %s and globalTaskUUID %s cannot be offloaded because of status change".formatted(taskSchedulingToBeOffloaded.getStatus(), taskSchedulingToBeOffloaded.getGlobalTaskUUID()));
            // TODO: exception?
        }
    }

    private CreateNewTaskOnPnaResponseDTO offloadToPNA(String globalTaskUUId, TaskScheduling taskScheduling) throws PnaApiException, TaskServiceException {
        // case SCHEDULED, to be offloaded
        if (taskScheduling.getStatus() == TaskStatus.SCHEDULED) {
            CreateNewTaskOnPnaDTO createNewTaskOnPna = CreateNewTaskOnPnaDTO.builder()
                    .globalTaskUUID(globalTaskUUId)
                    .applicationId(taskScheduling.getApplicationId())
                    .applicationComponentId(taskScheduling.getApplicationComponentId())
                    .payload(taskScheduling.getTask().getPayload())
                    .callbackProtocol(taskScheduling.getTask().getTaskMetaData().getCallbackProtocol())
                    .callbackEndpoint(taskScheduling.getTask().getTaskMetaData().getCallbackEndpoint())
                    .destinationApplicationComponentProtocol(taskScheduling.getTask().getTaskMetaData().getDestinationApplicationComponentProtocol())
                    .destinationApplicationComponentEndpoint(taskScheduling.getTask().getTaskMetaData().getDestinationApplicationComponentEndpoint())
                    .properties(taskScheduling.getTask().getProperties())
                    .build();
            // note that this is an async operation, task will only be created on remote device (blocking), task changes are incoming asynchronously
            return this.pnaApi.createNewTaskOnPna(taskScheduling.getNodeId(), createNewTaskOnPna);
        } else if (taskScheduling.getStatus() == TaskStatus.OFFLOADED) {
            logger.warn("Update after offloading not supported yet");
        }
        logger.warn("Offloading with status %s not supported".formatted(taskScheduling.getStatus()));
        throw new TaskServiceException("Offloading with status %s not supported".formatted(taskScheduling.getStatus()));
    }


    private void issueCompletedTaskToUser(Task task, TaskStatus taskStatus) {
        this.taskServiceChannel.send(new GenericMessage<>(TaskMessage.fromTask(task, taskStatus), new MessageHeaders(Map.of("mqtt_topic", "tasks/completed"))));
    }

    private void issueTaskStatusLogToPMS(TaskStatusLog savedTaskStatusLog, TaskScheduling taskScheduling) {
        this.taskServiceChannel.send(new GenericMessage<>(TaskStatusLogMessage.fromTaskStatusLog(savedTaskStatusLog, taskScheduling.getProperties()), new MessageHeaders(Map.of("mqtt_topic", "dt/pulceo/tasks"))));
    }

    private void issueEventToPMS(EventType eventType, TaskStatusLog savedTaskStatusLog) throws InterruptedException {
        this.eventHandler.handleEvent(PulceoEvent.builder()
                .eventType(eventType)
                .payload(savedTaskStatusLog.toString())
                .build());
    }

    private TaskStatusLog logStatusChange(TaskStatus previousStatus, String previousStateOfTask, TaskScheduling updatedTaskScheduling, Task task) {
        // create TaskStatusLog
        TaskStatusLog taskStatusLog = TaskStatusLog.builder()
                .previousStatus(previousStatus)
                .newStatus(updatedTaskScheduling.getStatus())
                .previousStateOfTask(previousStateOfTask)
                .newStateOfTask(updatedTaskScheduling.toString())
                .modifiedBy("psm")
                .modifiedById(PSM_UUID)
                // modified on automatically set
                .taskScheduling(updatedTaskScheduling)
                .task(task)
                .build();

        // add TaskStatusLog to TaskScheduling
        updatedTaskScheduling.addTaskStatusLog(taskStatusLog);
        return taskStatusLog;
    }

    // for updates from pna
    private TaskStatusLog logStatusChange(TaskStatus previousStatus, String previousStateOfTask, TaskScheduling updatedTaskScheduling, Task task, Timestamp modifiedOn, String modifiedBy) {
        TaskStatusLog taskStatusLog = this.logStatusChange(previousStatus, previousStateOfTask, updatedTaskScheduling, task);
        taskStatusLog.setModifiedById(modifiedBy);
        taskStatusLog.setModifiedBy("node");
        taskStatusLog.setModifiedOn(modifiedOn);
        return taskStatusLog;
    }

}
