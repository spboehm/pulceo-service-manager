package dev.pulceo.prm.service;

import dev.pulceo.prm.api.PnaApi;
import dev.pulceo.prm.api.dto.task.CreateNewTaskOnPnaDTO;
import dev.pulceo.prm.api.exception.PnaApiException;
import dev.pulceo.prm.exception.TaskServiceException;
import dev.pulceo.prm.model.task.Task;
import dev.pulceo.prm.model.task.TaskScheduling;
import dev.pulceo.prm.model.task.TaskStatusLog;
import dev.pulceo.prm.repository.TaskRepository;
import dev.pulceo.prm.repository.TaskSchedulingRepository;
import dev.pulceo.prm.repository.TaskStatusLogRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TaskService {

    private final Logger logger = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final TaskSchedulingRepository taskSchedulingRepository;
    private final TaskStatusLogRepository taskStatusLogRepository;
    private final PublishSubscribeChannel taskServiceChannel;
    private final EventHandler eventHandler;
    private final PnaApi pnaApi;


    @Autowired
    public TaskService(TaskRepository taskRepository, TaskSchedulingRepository taskSchedulingRepository, TaskStatusLogRepository taskStatusLogRepository, PublishSubscribeChannel taskServiceChannel, EventHandler eventHandler, PnaApi pnaApi) {
        this.taskRepository = taskRepository;
        this.taskSchedulingRepository = taskSchedulingRepository;
        this.taskStatusLogRepository = taskStatusLogRepository;
        this.taskServiceChannel = taskServiceChannel;
        this.eventHandler = eventHandler;
        this.pnaApi = pnaApi;
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
        this.taskServiceChannel.send(new GenericMessage<>(task));
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

        // update task scheduling
        taskScheduling.setNodeId(updatedTaskScheduling.getNodeId());
        taskScheduling.setApplicationId(updatedTaskScheduling.getApplicationId());
        taskScheduling.setApplicationComponentId(updatedTaskScheduling.getApplicationComponentId());
        taskScheduling.setStatus(updatedTaskScheduling.getStatus());
        taskScheduling.addTaskStatusLog(taskStatusLog);
        taskScheduling.addTask(task);

        // on status change via update
        try {
            schedule(taskScheduling);
        } catch (PnaApiException e) {
            throw new TaskServiceException("Task could not be scheduled", e);
        }

        // persist and return
        return this.taskSchedulingRepository.save(taskScheduling);
    }

    private void schedule(TaskScheduling taskScheduling) throws PnaApiException {

        /*
        private String nodeId = ""; // global node id where the task is scheduled
        private String applicationId = ""; // global application id
        private String applicationComponentId = ""; // global application component id
        private TaskStatus status = TaskStatus.NEW; // task status
        private Task task; // task
        private List<TaskStatusLog> statusLogs; // task status logs
        */


        this.pnaApi.createNewTaskOnPna("", CreateNewTaskOnPnaDTO.builder().build());

    }

    /* TaskStatusLogs */
    public List<TaskStatusLog> readAllTaskStatusLogsByTaskId(Long id) {
        List<TaskStatusLog> taskStatusLogs = this.taskStatusLogRepository.findTaskStatusLogsByTaskId(id);
        if (taskStatusLogs.isEmpty()) {
            return new ArrayList<>();
        }
        return taskStatusLogs;
    }
}
