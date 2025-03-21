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
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
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
    // TODO: Bean-based access and configuration
    // uuid of task scheduling
    private final BlockingQueue<String> taskSchedulingQueue = new ArrayBlockingQueue<>(1000);
    private final ThreadPoolTaskScheduler threadPoolTaskScheduler;


    @Autowired
    public TaskService(TaskRepository taskRepository, TaskSchedulingRepository taskSchedulingRepository, TaskStatusLogRepository taskStatusLogRepository, PublishSubscribeChannel taskServiceChannel, EventHandler eventHandler, PnaApi pnaApi, ThreadPoolTaskExecutor threadPoolTaskExecutor, BlockingQueue<Message<?>> mqttBlockingQueueTasksFromPna, ThreadPoolTaskScheduler threadPoolTaskScheduler) {
        this.taskRepository = taskRepository;
        this.taskSchedulingRepository = taskSchedulingRepository;
        this.taskStatusLogRepository = taskStatusLogRepository;
        this.taskServiceChannel = taskServiceChannel;
        this.eventHandler = eventHandler;
        this.pnaApi = pnaApi;
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        this.mqttBlockingQueueTasksFromPna = mqttBlockingQueueTasksFromPna;
        this.threadPoolTaskScheduler = threadPoolTaskScheduler;
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
        taskScheduling.setGlobalTaskUUID(task.getUuid().toString());
        taskScheduling.addTask(task);
        task.setTaskScheduling(taskScheduling);

        // TODO create temporary object to be persisted and return immediately

        /* TODO: make persistence non-blocking by invoking thread pooling with the interaction of redis and sql */

        // put to redis

        // put to sql db

        // TODO: NONE to NEW?, Add to Task Status Log?

        // TODO: broadcast to listener of task, e.g., via MQTT
        // TODO: broadcast to user, mqtt endpoint "tasks/", todo implement this for the general case
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
    public TaskScheduling updateTaskScheduling(UUID taskUUID, TaskScheduling updatedTaskScheduling) throws TaskServiceException, PnaApiException {
        Task task = this.taskRepository.findByUuid(taskUUID).orElseThrow();
        TaskScheduling taskScheduling = task.getTaskScheduling();
        String oldTaskStatus = taskScheduling.toString();

        // TODO: if no status change, return, idempotency;
        if (taskScheduling.getStatus() == updatedTaskScheduling.getStatus()) {
            return taskScheduling;
        }

        // TODO: implement NEW->SCHEDULED
        if (taskScheduling.getStatus() == TaskStatus.NEW && updatedTaskScheduling.getStatus() == TaskStatus.SCHEDULED) {
            logger.info("Schedule NEW task with id %s".formatted(taskScheduling.getUuid()));
            // only update properties from DTO, other information a filled async
            // TODO: add global taskId
            taskScheduling.setGlobalTaskUUID(task.getUuid().toString());
            // TODO: check if nodeId exists
            taskScheduling.setNodeId(updatedTaskScheduling.getNodeId());
            // TODO: check if applicationId exists
            taskScheduling.setApplicationId(updatedTaskScheduling.getApplicationId());
            // TODO: check if applicationComponentId exists
            taskScheduling.setApplicationComponentId(updatedTaskScheduling.getApplicationComponentId());
            // TODO: set new task status to scheduled
            taskScheduling.setStatus(TaskStatus.SCHEDULED);
            // issue new task to be scheduled to background thread via blocking queue
            // TODO: maybe add task id resolver
            // TODO: add log
            taskScheduling.addTask(task);
            taskScheduling.addTaskStatusLog(this.logStatusChange(TaskStatus.NEW, oldTaskStatus, updatedTaskScheduling, task));
            this.taskSchedulingRepository.save(taskScheduling);
            this.taskSchedulingQueue.add(taskScheduling.getUuid().toString());
            return taskScheduling;
        } else {
            throw new TaskServiceException("Status change not supported (yet)...");
        }

        // TODO: After NEW->SCHEDULED, implement SCHEDULED->OFFLOADED in a asynchronous operation
    }

    private TaskStatusLog logStatusChange(TaskStatus previousStatus, String previousStateOfTask, TaskScheduling updatedTaskScheduling, Task task) {
        // create TaskStatusLog
        TaskStatusLog taskStatusLog = TaskStatusLog.builder()
                .previousStatus(previousStatus)
                .newStatus(updatedTaskScheduling.getStatus())
                .previousStateOfTask(previousStateOfTask)
                .newStateOfTask(updatedTaskScheduling.toString())
                .taskScheduling(updatedTaskScheduling)
                .task(task)
                .build();

        // add TaskStatusLog to TaskScheduling
        updatedTaskScheduling.addTaskStatusLog(taskStatusLog);
        return taskStatusLog;
    }

    // TOOD: remove?
    private void scheduleTask(TaskScheduling taskScheduling, TaskScheduling updatedTaskScheduling) throws TaskServiceException {
        // TODO: put to taskSchedulingQueue
    }

    private void offloadScheduledTasks(String taskSchedulingId) throws InterruptedException, PnaApiException {
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

        // check if task has not been scheduled yet
        if (taskSchedulingToBeOffloaded.getStatus() == TaskStatus.SCHEDULED) {
            logger.info("TaskScheduling to be offloaded has payload %s".formatted(taskSchedulingToBeOffloaded.toString()));
            // TODO: Offload to corresponding PNA
            CreateNewTaskOnPnaResponseDTO createNewTaskOnPnaResponseDTO = offloadToPNA(taskSchedulingToBeOffloaded.getTask().getUuid().toString(), taskSchedulingToBeOffloaded);
            // global task UUID already set
            taskSchedulingToBeOffloaded.setGlobalTaskUUID(createNewTaskOnPnaResponseDTO.getGlobalTaskUUID());
            taskSchedulingToBeOffloaded.setRemoteTaskUUID(createNewTaskOnPnaResponseDTO.getRemoteTaskUUID().toString());
            taskSchedulingToBeOffloaded.setRemoteNodeUUID(createNewTaskOnPnaResponseDTO.getRemoteNodeUUID().toString());
            // TODO: Persist changes to DB, that task is offloaded, maybe change to when task is really offloaded, asumption is offloaded
            taskSchedulingToBeOffloaded.setStatus(TaskStatus.OFFLOADED);
//            taskSchedulingToBeOffloaded.addTaskStatusLog(this.logStatusChange(TaskStatus.NEW, oldTaskStatus, taskSchedulingToBeOffloaded, taskSchedulingToBeOffloaded.getTask()));
            this.taskSchedulingRepository.save(taskSchedulingToBeOffloaded);
            // Persis task scheduling logs
            Optional<Task> taskOptional = this.taskRepository.findByUuid(taskSchedulingToBeOffloaded.getTask().getUuid());
            this.taskStatusLogRepository.save(this.logStatusChange(TaskStatus.SCHEDULED, oldTaskStatus, taskSchedulingToBeOffloaded, taskOptional.get()));
            logger.info("Successfully offloaded task with id %s".formatted(taskSchedulingId));
        } else {
            logger.warn("Task with status %s cannot be offloaded because of status change".formatted(taskSchedulingToBeOffloaded.getStatus()));
        }
    }

    private CreateNewTaskOnPnaResponseDTO offloadToPNA(String globalTaskUUId, TaskScheduling taskScheduling) throws PnaApiException {
        // case SCHEDULED or OFFLOADED
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

            // TODO: issue event to the "tasks/" endpoint
            // note that this is an async operation, task will only be created on remote device, task changes are incoming
            return this.pnaApi.createNewTaskOnPna(taskScheduling.getNodeId(), createNewTaskOnPna);
        } else if (taskScheduling.getStatus() == TaskStatus.OFFLOADED) {
            logger.warn("Update after offloading not implemented yet");
        } // TODO: further cases
        logger.warn("TaskScheduling status not supported");
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

        if (updateTaskFromPNADTO.getNewTaskStatus() == TaskStatus.RUNNING) {
            // TODO: get task
            // TODO: TaskScheduling would be sufficient?
            logger.info("Updating task from PNA with %s".formatted(updateTaskFromPNADTO.toString()));
            Optional<Task> taskOptional = this.taskRepository.findByUuid(UUID.fromString(updateTaskFromPNADTO.getGlobalTaskUUID()));
            if (taskOptional.isEmpty()) {
                logger.warn("Task not found");
                return;
            }
            Task task = taskOptional.get();

            // retrieve task scheduling
            Optional<TaskScheduling> taskScheduling = this.taskSchedulingRepository.findWithStatusLogsByUuid(task.getTaskScheduling().getUuid());
            System.out.println(taskScheduling.get().getStatus());
            if (taskScheduling.isEmpty()) {
                logger.warn("Task not found");
                // TODO: abort
                throw new TaskServiceException("Associated TaskScheduling not found");
            }
            TaskStatus previousTaskStatus = taskScheduling.get().getStatus();

            TaskScheduling taskSchedulingToBeUpdated = taskScheduling.get();
            String stateOfTaskScheduling = taskSchedulingToBeUpdated.toString();
            taskSchedulingToBeUpdated.setStatus(updateTaskFromPNADTO.getNewTaskStatus());
            // create new TaskStatusLog
            // TODO: reflect modified by String modifiedBy
            // TODO: reflect modified on Timestamp modifiedOn
            // TODO: update newTaskStatus in TaskScheduling
            this.taskStatusLogRepository.save(this.logStatusChange(previousTaskStatus, stateOfTaskScheduling, taskSchedulingToBeUpdated, task));
            // TODO: broadcast to users
        }


    }
    // TODO: mqtt listener for task status changes, issued by PNA, using mqtt with topic "cmd/pulceo/tasks"

    // TODO: stop svc

    @PostConstruct
    public void init() {
        threadPoolTaskExecutor.submit(() -> {
            logger.info("Initializing task service...");
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
                                logger.info("Received update for task %s".formatted(updateTaskFromPNADTO.getRemoteTaskUUID()));
                                this.updateTaskFromPna(updateTaskFromPNADTO);
                            } else {
                                logger.warn("Unsupported operation for resource type TASK");
                            }
                            break;
                        default:
                            logger.warn("Unsupported resource type");
                    }
                } catch (TaskServiceException e) {
                    logger.warn(e.getMessage());
                } catch (JsonProcessingException e) {
                    logger.warn("Couldn't parse payload...continue", e);
                } catch (InterruptedException e) {
                    logger.info("Initiate shutdown of TaskService...");
                    this.isRunning.set(false);
                }
            }
        });
        threadPoolTaskExecutor.submit(() -> {
            logger.info("Initializing task scheduling service...");
            while (isRunning.get()) {
                try {
                    logger.info("TaskService is waiting for scheduling tasks");
                    // TODO: retrieve vom BlockingQueue
                    String taskSchedulingUuid = this.taskSchedulingQueue.take();
                    threadPoolTaskScheduler.submit(() -> {
                        try {
                            this.offloadScheduledTasks(taskSchedulingUuid);
                        } catch (InterruptedException | PnaApiException e) {
                            throw new RuntimeException(e);
                        }
                    });
                } catch (InterruptedException e) {
                    logger.info("Initiate shutdown of task scheduling service...");
                    this.isRunning.set(false);
                }
            }
        });
    }

}
