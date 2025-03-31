package dev.pulceo.prm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pulceo.prm.api.PnaApi;
import dev.pulceo.prm.api.PrmApi;
import dev.pulceo.prm.api.dto.task.CreateNewTaskOnPnaDTO;
import dev.pulceo.prm.api.dto.task.CreateNewTaskOnPnaResponseDTO;
import dev.pulceo.prm.api.exception.PnaApiException;
import dev.pulceo.prm.api.exception.PrmApiException;
import dev.pulceo.prm.dto.message.Operation;
import dev.pulceo.prm.dto.message.ResourceMessage;
import dev.pulceo.prm.dto.task.UpdateTaskFromPNADTO;
import dev.pulceo.prm.exception.TaskServiceException;
import dev.pulceo.prm.model.event.EventType;
import dev.pulceo.prm.model.event.PulceoEvent;
import dev.pulceo.prm.model.message.TaskStatusLogMessage;
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
import org.springframework.beans.factory.annotation.Value;
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
    private final PrmApi prmApi;
    private final ObjectMapper objectMapper = new ObjectMapper();
    // TODO: Bean-based access and configuration
    // uuid of task scheduling
    private final BlockingQueue<String> taskSchedulingQueue = new ArrayBlockingQueue<>(1000);
    private final ThreadPoolTaskScheduler threadPoolTaskScheduler;
    private final TaskStatisticsService taskStatisticsService;

    @Value("${psm.uuid}")
    private String PSM_UUID;

    @Autowired
    public TaskService(TaskRepository taskRepository, TaskSchedulingRepository taskSchedulingRepository, TaskStatusLogRepository taskStatusLogRepository, PublishSubscribeChannel taskServiceChannel, EventHandler eventHandler, PnaApi pnaApi, PrmApi prmApi, ThreadPoolTaskExecutor threadPoolTaskExecutor, BlockingQueue<Message<?>> mqttBlockingQueueTasksFromPna, ThreadPoolTaskScheduler threadPoolTaskScheduler, TaskStatisticsService taskStatisticsService) {
        this.taskRepository = taskRepository;
        this.taskSchedulingRepository = taskSchedulingRepository;
        this.taskStatusLogRepository = taskStatusLogRepository;
        this.taskServiceChannel = taskServiceChannel;
        this.eventHandler = eventHandler;
        this.pnaApi = pnaApi;
        this.prmApi = prmApi;
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        this.mqttBlockingQueueTasksFromPna = mqttBlockingQueueTasksFromPna;
        this.threadPoolTaskScheduler = threadPoolTaskScheduler;
        this.taskStatisticsService = taskStatisticsService;
    }

    public Task createTask(Task task) throws InterruptedException {
        logger.info("Creating task {}", task);

        // log to statistics
        long taskSequenceNumber = this.taskStatisticsService.incrementTaskNumberAndGet();
        task.setTaskSequenceNumber(taskSequenceNumber);
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
        String previousStateOfTaskScheduling = taskScheduling.toString();
        taskScheduling.setStatus(TaskStatus.NEW);
        taskScheduling.setGlobalTaskUUID(task.getUuid().toString());
        taskScheduling.addTask(task);
        task.setTaskScheduling(taskScheduling);

        // TODO create temporary object to be persisted and return immediately

        /* TODO: make persistence non-blocking by invoking thread pooling with the interaction of redis and sql */

        // put to redis

        // put to sql db
        Task savedTask = this.taskRepository.save(task);
//        TaskScheduling savedTaskScheduling = this.taskSchedulingRepository.save(taskScheduling);
        TaskStatusLog taskStatusLog = this.logStatusChange(TaskStatus.NONE, previousStateOfTaskScheduling, savedTask.getTaskScheduling(), savedTask);
        TaskStatusLog savedTaskStatusLog = this.taskStatusLogRepository.save(taskStatusLog);
        // publish event to PMS via MQTT
        issueEventToPMS(EventType.TASK_CREATED, savedTaskStatusLog);
        // publish task status log to pms via MQTT
        issueTaskStatusLogToPMS(savedTaskStatusLog);
        this.logger.debug("Send task status log message {} to PMS via MQTT", savedTaskStatusLog);
        // TODO: In case of status changes, schedule task directly
        return savedTask;
    }

    private void issueTaskStatusLogToPMS(TaskStatusLog savedTaskStatusLog) {
        this.taskServiceChannel.send(new GenericMessage<>(TaskStatusLogMessage.fromTaskStatusLog(savedTaskStatusLog), new MessageHeaders(Map.of("mqtt_topic", "dt/pulceo/tasks"))));
    }

    private void issueEventToPMS(EventType eventType, TaskStatusLog savedTaskStatusLog) throws InterruptedException {
        this.eventHandler.handleEvent(PulceoEvent.builder()
                .eventType(eventType)
                .payload(savedTaskStatusLog.toString())
                .build());
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
    public Optional<TaskScheduling> readTaskSchedulingByTaskId(String id) throws TaskServiceException {
        Optional<TaskScheduling> taskSchedulingOptional = this.taskSchedulingRepository.findByTaskUuid(UUID.fromString(id));
        if (taskSchedulingOptional.isEmpty()) {
            throw new TaskServiceException("TaskScheduling with id %s not found".formatted(id));
        }
        return taskSchedulingOptional;
    }


    @Transactional
    public TaskScheduling updateTaskScheduling(UUID taskUUID, TaskScheduling updatedTaskScheduling) throws TaskServiceException, PnaApiException, InterruptedException {
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
            // TODO: order of save of TaskStatusLog?
            taskScheduling.addTask(task);
            TaskStatusLog taskStatusLogScheduled = this.logStatusChange(TaskStatus.NEW, oldTaskStatus, updatedTaskScheduling, task);
            taskScheduling.addTaskStatusLog(taskStatusLogScheduled);
            TaskScheduling savedTaskScheduling = this.taskSchedulingRepository.save(taskScheduling);
            // publish event to PMS via MQTT
            issueEventToPMS(EventType.TASK_SCHEDULED, taskStatusLogScheduled);
            // publish task status log to pms via MQTT
            issueTaskStatusLogToPMS(taskStatusLogScheduled);
            this.taskSchedulingQueue.add(taskScheduling.getUuid().toString());
            return savedTaskScheduling;
        } else {
            throw new TaskServiceException("Status change not supported (yet)...");
        }
    }

    // for psm
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
        logger.debug("Try to schedule task scheduling with old state " + oldTaskStatus);

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

            // TODO: issue to task endpoint

            // note that this is an async operation, task will only be created on remote device, task changes are incoming
            return this.pnaApi.createNewTaskOnPna(taskScheduling.getNodeId(), createNewTaskOnPna);
        } else if (taskScheduling.getStatus() == TaskStatus.OFFLOADED) {
            logger.warn("Update after offloading not implemented yet");
        } // TODO: further cases
        logger.warn("TaskScheduling status not supported");
        return CreateNewTaskOnPnaResponseDTO.builder().build();
    }

    /* TaskStatusLogs */

    // TODO: add by task uuid

    // TODO: add by taskscheduling uuid

    public List<TaskStatusLog> readAllTaskStatusLogsByTaskId(Long id) {
        List<TaskStatusLog> taskStatusLogs = this.taskStatusLogRepository.findTaskStatusLogsByTaskId(id);
        if (taskStatusLogs.isEmpty()) {
            return new ArrayList<>();
        }
        return taskStatusLogs;
    }

    private void updateTaskFromPna(String pnaUUID, UpdateTaskFromPNADTO updateTaskFromPNADTO) throws TaskServiceException {
        logger.info("Updating task, received from PNA with payload %s".formatted(updateTaskFromPNADTO.toString()));
        if (updateTaskFromPNADTO.getNewTaskStatus() == TaskStatus.RUNNING || updateTaskFromPNADTO.getNewTaskStatus() == TaskStatus.COMPLETED) {
            // TODO: TaskScheduling would be sufficient?
            Optional<Task> taskOptional = this.taskRepository.findByUuid(UUID.fromString(updateTaskFromPNADTO.getGlobalTaskUUID()));
            if (taskOptional.isEmpty()) {
                logger.warn("Task not found");
                return;
            }
            Task task = taskOptional.get();

            // retrieve task scheduling
            Optional<TaskScheduling> taskScheduling = this.taskSchedulingRepository.findWithStatusLogsByUuid(task.getTaskScheduling().getUuid());
            if (taskScheduling.isEmpty()) {
                logger.warn("Task not found");
                // TODO: abort
                throw new TaskServiceException("Associated TaskScheduling not found");
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
                this.taskStatusLogRepository.save(this.logStatusChange(previousTaskStatus, stateOfTaskScheduling, taskSchedulingToBeUpdated, task, updateTaskFromPNADTO.getModifiedOn(), globalId));
            } catch (PrmApiException e) {
                throw new RuntimeException(e);
            }
            // TODO: broadcast to users

            // TODO: broadcast to event endpoint pms
        } else {
            this.logger.warn("Unsupported task status, received status %s".formatted(updateTaskFromPNADTO.getNewTaskStatus()));
        }
    }

    @PostConstruct
    public void init() {
        // TODO: mqtt listener for task status changes, issued by PNA, using mqtt with topic "cmd/pulceo/tasks"
        // TODO: stop svc
        // for messages received via mqtt
        threadPoolTaskExecutor.submit(() -> {
            logger.info("Initializing task service...");
            while (isRunning.get()) {
                try {
                    logger.info("Task service is waiting for messages...");
                    Message<?> message = mqttBlockingQueueTasksFromPna.take();
                    logger.info("TaskService received message via mqtt: " + message.getPayload());
                    String payload = (String) message.getPayload();
                    ResourceMessage resourceMessage = objectMapper.readValue(payload, ResourceMessage.class);

                    // decide on the message type
                    switch (resourceMessage.getResourceType()) {
                        case TASK:
                            if (resourceMessage.getOperation() == Operation.UPDATE) {
                                logger.info("Update received from %s".formatted(resourceMessage.getSentBydeviceId()));
                                UpdateTaskFromPNADTO updateTaskFromPNADTO = objectMapper.readValue(resourceMessage.getPayload(), UpdateTaskFromPNADTO.class);
                                logger.info("Received update for task %s".formatted(updateTaskFromPNADTO.getRemoteTaskUUID()));
                                this.updateTaskFromPna(resourceMessage.getSentBydeviceId(), updateTaskFromPNADTO);
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
