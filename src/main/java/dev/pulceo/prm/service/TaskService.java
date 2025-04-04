package dev.pulceo.prm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pulceo.prm.api.PnaApi;
import dev.pulceo.prm.api.PrmApi;
import dev.pulceo.prm.api.exception.PnaApiException;
import dev.pulceo.prm.dto.message.Operation;
import dev.pulceo.prm.dto.message.ResourceMessage;
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
    private final TaskOffloader taskOffloader;

    @Value("${psm.uuid}")
    private String PSM_UUID;

    @Autowired
    public TaskService(TaskRepository taskRepository, TaskSchedulingRepository taskSchedulingRepository, TaskStatusLogRepository taskStatusLogRepository, PublishSubscribeChannel taskServiceChannel, EventHandler eventHandler, PnaApi pnaApi, PrmApi prmApi, ThreadPoolTaskExecutor threadPoolTaskExecutor, BlockingQueue<Message<?>> mqttBlockingQueueTasksFromPna, ThreadPoolTaskScheduler threadPoolTaskScheduler, TaskStatisticsService taskStatisticsService, TaskOffloader taskOffloader) {
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
        this.taskOffloader = taskOffloader;
    }

    @Transactional
    public Task createTask(Task task, Map<String, String> schedulingProperties) throws InterruptedException {
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
        taskScheduling.setProperties(schedulingProperties);
        // TODO: inject sent scheduling properties into taskScheduling
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
        issueEventToPMS(EventType.fromTaskStatus(taskScheduling.getStatus()), savedTaskStatusLog);
        // publish task status log to pms via MQTT
        issueTaskStatusLogToPMS(savedTaskStatusLog, taskScheduling);
        // issue to user
        issueNewTaskToUser(savedTask);
        this.logger.debug("Send task status log message {} to PMS via MQTT", savedTaskStatusLog);
        // TODO: In case of status changes, schedule task directly
        return savedTask;
    }

    private void issueNewTaskToUser(Task task) {
        this.taskServiceChannel.send(new GenericMessage<>(TaskMessage.fromTask(task), new MessageHeaders(Map.of("mqtt_topic", "tasks/new"))));
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
            logger.info("Schedule NEW task with globalTaskUUID %s".formatted(taskUUID));
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
            // set properties
            //taskScheduling.setProperties(updatedTaskScheduling.getProperties());
            // issue new task to be scheduled to background thread via blocking queue
            // TODO: order of save of TaskStatusLog?
            taskScheduling.addTask(task);
            TaskStatusLog taskStatusLogScheduled = this.logStatusChange(TaskStatus.NEW, oldTaskStatus, updatedTaskScheduling, task);
            taskScheduling.addTaskStatusLog(taskStatusLogScheduled);
            TaskScheduling savedTaskScheduling = this.taskSchedulingRepository.save(taskScheduling);
            // publish event to PMS via MQTT
            issueEventToPMS(EventType.fromTaskStatus(taskScheduling.getStatus()), taskStatusLogScheduled);
            // publish task status log to pms via MQTT
            issueTaskStatusLogToPMS(taskStatusLogScheduled, taskScheduling);
            logger.info("Task scheduling with status {} added to queue with UUID: {}", savedTaskScheduling.getStatus(), savedTaskScheduling.getGlobalTaskUUID());
            return savedTaskScheduling;
        } else {
            throw new TaskServiceException("Status change not supported (yet)...");
        }
    }

    public void queueForScheduling(String taskSchedulingUuid) {
        this.taskSchedulingQueue.add(taskSchedulingUuid);
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
                                this.taskOffloader.updateTaskFromPna(resourceMessage.getSentBydeviceId(), updateTaskFromPNADTO);
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
                } catch (Exception e) {
                    logger.error("Error in TaskService: ", e);
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
                            this.taskOffloader.offloadScheduledTasks(taskSchedulingUuid);
                        } catch (InterruptedException | PnaApiException | TaskServiceException e) {
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
