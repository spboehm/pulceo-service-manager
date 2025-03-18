package dev.pulceo.prm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pulceo.prm.api.PnaApi;
import dev.pulceo.prm.api.dto.task.CreateNewTaskOnPnaDTO;
import dev.pulceo.prm.api.dto.task.CreateNewTaskOnPnaResponseDTO;
import dev.pulceo.prm.api.exception.PnaApiException;
import dev.pulceo.prm.dto.message.Operation;
import dev.pulceo.prm.dto.message.ResourceMessage;
import dev.pulceo.prm.dto.task.UpdateTaskFromPNADTO;
import dev.pulceo.prm.exception.TaskServiceException;
import dev.pulceo.prm.model.task.Task;
import dev.pulceo.prm.model.task.TaskScheduling;
import dev.pulceo.prm.model.task.TaskStatus;
import dev.pulceo.prm.model.task.TaskStatusLog;
import dev.pulceo.prm.repository.TaskRepository;
import dev.pulceo.prm.repository.TaskSchedulingRepository;
import dev.pulceo.prm.repository.TaskStatusLogRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class TaskService {

    private final Logger logger = LoggerFactory.getLogger(TaskService.class);

    private final BlockingQueue<Message<?>> mqttBlockingQueueTasksFromPna;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final TaskRepository taskRepository;
    private final TaskSchedulingRepository taskSchedulingRepository;
    private final TaskStatusLogRepository taskStatusLogRepository;
    private final PublishSubscribeChannel taskServiceChannel;
    private final EventHandler eventHandler;
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final PnaApi pnaApi;
    private final ObjectMapper objectMapper = new ObjectMapper();


    @Autowired
    public TaskService(TaskRepository taskRepository, TaskSchedulingRepository taskSchedulingRepository, TaskStatusLogRepository taskStatusLogRepository, PublishSubscribeChannel taskServiceChannel, EventHandler eventHandler, PnaApi pnaApi, ThreadPoolTaskExecutor threadPoolTaskExecutor, BlockingQueue<Message<?>> mqttBlockingQueueTasksFromPna) {
        this.taskRepository = taskRepository;
        this.taskSchedulingRepository = taskSchedulingRepository;
        this.taskStatusLogRepository = taskStatusLogRepository;
        this.taskServiceChannel = taskServiceChannel;
        this.eventHandler = eventHandler;
        this.pnaApi = pnaApi;
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        this.mqttBlockingQueueTasksFromPna = mqttBlockingQueueTasksFromPna;
    }

    public Task createTask(Task task) throws InterruptedException {

        // TODO: validation of Task

        // TODO: check for duplicates

        // enrich with additional information
        task.setArrived(Timestamp.valueOf(LocalDateTime.now()));
        // recalculate size of workload
        if (task.getSizeOfWorkload() == 0) {
            // TODO: calculate size of workload
            task.setSizeOfWorkload(task.getPayload().length);
        }
        // recalculate sizeDuringTransmission
        if (task.getSizeDuringTransmission() == 0) {
            // TODO: calculate size during transmission
            task.setSizeDuringTransmission(task.getPayload().length);
        }

        // TODO: set task scheduling references
        TaskScheduling taskScheduling = TaskScheduling.builder().build();
        taskScheduling.addTask(task);
        task.setTaskScheduling(taskScheduling);

        // TODO create temporary object to be persisted and return immediately

        /* TODO: make persistence non-blocking by invoking thread pooling with the interaction of redis and sql */

        // put to redis

        // put to sql db

        // TODO: broadcast to listener of task, e.g., via MQTT
        Task savedTask = this.taskRepository.save(task);
        // broadcast to user, mqtt endpoint "tasks/", todo implement this for the general case
        this.taskServiceChannel.send(new GenericMessage<>(task, new MessageHeaders(Map.of("mqtt_topic", "tasks/"))));
        /*
        this.eventHandler.handleEvent(PulceoEvent.builder()
                .eventType(EventType.APP)
                .payload("test")
                .build());
        */

        // TODO: In case of status changes, schedule task directly

        return savedTask;
    }

    @Transactional
    public Optional<Task> readTaskByUUID(UUID taskUUID) {
        return this.taskRepository.findByUuid((taskUUID));
    }

    public List<Task> readAllTasks() {
        List<Task> tasks = new ArrayList<>();
        Iterable<Task> taskList = this.taskRepository.findAll();
        taskList.forEach(task -> tasks.add(task));
        return tasks;
    }

    /* Task Scheduling */
    @Transactional
    public TaskScheduling updateTaskScheduling(UUID taskUUID, TaskScheduling updatedTaskScheduling) throws TaskServiceException {
        Task task = this.taskRepository.findByUuid(taskUUID).orElseThrow();
        TaskScheduling taskScheduling = task.getTaskScheduling();

        // add to TaskStatusLog history of TaskScheduling
        TaskStatusLog taskStatusLog = TaskStatusLog.builder()
                .previousStatus(taskScheduling.getStatus())
                .newStatus(updatedTaskScheduling.getStatus())
                .previousStateOfTask(taskScheduling.toString())
                .newStateOfTask(updatedTaskScheduling.toString())
                .taskScheduling(taskScheduling)
                .task(task)
                .build();

        // update task scheduling, remove if UpdateTaskDTO is ready
        taskScheduling.setNodeId(updatedTaskScheduling.getNodeId());
        taskScheduling.setApplicationId(updatedTaskScheduling.getApplicationId());
        taskScheduling.setApplicationComponentId(updatedTaskScheduling.getApplicationComponentId());
        taskScheduling.setStatus(updatedTaskScheduling.getStatus());
        taskScheduling.addTaskStatusLog(taskStatusLog);
        taskScheduling.addTask(task);

        // on status change
        try {
            CreateNewTaskOnPnaResponseDTO createNewTaskOnPnaResponseDTO = schedule(taskScheduling);
            task.setRemoteTaskUUID(createNewTaskOnPnaResponseDTO.getRemoteTaskUUID().toString());
            taskScheduling.setRemoteNodeUUID(createNewTaskOnPnaResponseDTO.getRemoteNodeUUID().toString());
            taskScheduling.setRemoteTaskUUID(createNewTaskOnPnaResponseDTO.getRemoteTaskUUID().toString());
            taskScheduling.setStatus(createNewTaskOnPnaResponseDTO.getStatus());
            // TODO: do we need this here?
            //taskScheduling.addTaskStatusLog(taskStatusLog);
            // persist and return
            this.taskRepository.save(task); // because of remoteUUIDassignment
            return this.taskSchedulingRepository.save(taskScheduling);
        } catch (PnaApiException e) {
            throw new TaskServiceException("Could note schedule task!", e);
        }
    }

    private CreateNewTaskOnPnaResponseDTO schedule(TaskScheduling taskScheduling) throws PnaApiException {
        // case SCHEDULED or OFFLOADED
        if (taskScheduling.getStatus() == TaskStatus.SCHEDULED) {
            CreateNewTaskOnPnaDTO createNewTaskOnPna = CreateNewTaskOnPnaDTO.builder()
                    .applicationId(taskScheduling.getApplicationId())
                    .applicationComponentId(taskScheduling.getApplicationComponentId())
                    .payload(taskScheduling.getTask().getPayload())
                    .callbackProtocol(taskScheduling.getTask().getTaskMetaData().getCallbackProtocol())
                    .callbackEndpoint(taskScheduling.getTask().getTaskMetaData().getCallbackEndpoint())
                    .destinationApplicationComponentProtocol(taskScheduling.getTask().getTaskMetaData().getDestinationApplicationComponentProtocol())
                    .destinationApplicationComponentEndpoint(taskScheduling.getTask().getTaskMetaData().getDestinationApplicationComponentEndpoint())
                    .properties(taskScheduling.getTask().getProperties())
                    .build();

            // TODO: issue event to the "tasks/" endpoint

            return this.pnaApi.createNewTaskOnPna(taskScheduling.getNodeId(), createNewTaskOnPna);
        } else if (taskScheduling.getStatus() == TaskStatus.OFFLOADED) {
            logger.warn("Update after offloading not implemented yet");
        } // TODO: further cases
        return CreateNewTaskOnPnaResponseDTO.builder().build();
    }

    /* TaskStatusLogs */
    public List<TaskStatusLog> readAllTaskStatusLogsByTaskId(Long id) {
        List<TaskStatusLog> taskStatusLogs = this.taskStatusLogRepository.findTaskStatusLogsByTaskId(id);
        if (taskStatusLogs.isEmpty()) {
            return new ArrayList<>();
        }
        return taskStatusLogs;
    }


    private void updateTaskFromPna(UpdateTaskFromPNADTO updateTaskFromPNADTO) throws TaskServiceException {

        // TODO: get task
        // TODO: TaskScheduling would be sufficient?
        Optional<Task> taskOptional = this.taskRepository.findByRemoteTaskUUID(updateTaskFromPNADTO.getRemoteTaskUUID());
        if (taskOptional.isEmpty()) {
            logger.warn("Task not found");
            return;
        }
        Task taskToBeUpdated = taskOptional.get();

        // retrieve task scheduling
        Optional<TaskScheduling> taskScheduling = this.taskSchedulingRepository.findWithStatusLogsByUuid(taskToBeUpdated.getTaskScheduling().getUuid());

        if (taskScheduling.isEmpty()) {
            logger.warn("Task not found");
            // TODO: abort
            throw new TaskServiceException("Associated TaskScheduling not found");
        }

        TaskScheduling taskSchedulingToBeUpdated = taskScheduling.get();
        taskSchedulingToBeUpdated.setStatus(updateTaskFromPNADTO.getNewTaskStatus());
        // create new TaskStatusLog
        // TODO: reflect modified by String modifiedBy
        // TODO: reflect modified on Timestamp modifiedOn
        // TODO: update newTaskStatus in TaskScheduling
        TaskStatusLog taskStatusLog = TaskStatusLog.builder()
                .previousStatus(taskSchedulingToBeUpdated.getStatus())
                .newStatus(updateTaskFromPNADTO.getNewTaskStatus())
                .modifiedBy(updateTaskFromPNADTO.getModifiedByRemoteNodeUUID())
                .modifiedOn(updateTaskFromPNADTO.getModifiedOn())
                .task(taskToBeUpdated)
                .taskScheduling(taskSchedulingToBeUpdated)
                .build();

        // TODO: add TaskStatusLog to TaskScheduling
        taskSchedulingToBeUpdated.addTaskStatusLog(taskStatusLog);
        // TODO: broadcast to users

        // persis TaskScheduling
        // this.taskSchedulingRepository.save(taskSchedulingToBeUpdated);
    }
    // TODO: mqtt listener for task status changes, issued by PNA, using mqtt with topic "cmd/pulceo/tasks"

    // TODO: stop svc

    @PostConstruct
    public void init() {
        threadPoolTaskExecutor.execute(() -> {
            logger.info("Initializing Task Service...");
            while (isRunning.get()) {
                try {
                    logger.info("Task service is waiting for messages...");
                    Message<?> message = mqttBlockingQueueTasksFromPna.take();
                    logger.info("TaskService received message: " + message.getPayload());
                    String payload = (String) message.getPayload();
                    ResourceMessage resourceMessage = objectMapper.readValue(payload, ResourceMessage.class);

                    // decide on the message type
                    switch (resourceMessage.getResourceType()) {
                        case TASK:
                            if (resourceMessage.getOperation() == Operation.UPDATE) {
                                logger.info("Update received from %s".formatted(resourceMessage.getSentBydeviceId()));
                                UpdateTaskFromPNADTO updateTaskFromPNADTO = objectMapper.readValue(resourceMessage.getPayload(), UpdateTaskFromPNADTO.class);
                                this.updateTaskFromPna(updateTaskFromPNADTO);
                            } else {
                                logger.warn("Unsupported operation for resource type TASK");
                            }
                            break;
                        default:
                            logger.warn("Unsupported resource type");
                    }
                } catch (TaskServiceException e) {
                    logger.error(e.getMessage());
                } catch (JsonProcessingException e) {
                    logger.warn("Couldn't parse payload...continue", e);
                } catch (InterruptedException e) {
                    logger.info("Initiate shutdown of TaskService...");
                    this.isRunning.set(false);
                }
            }
        });
    }

}
