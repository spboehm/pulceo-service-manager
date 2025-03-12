package dev.pulceo.prm.service;

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

    @Autowired
    public TaskService(TaskRepository taskRepository, TaskSchedulingRepository taskSchedulingRepository, TaskStatusLogRepository taskStatusLogRepository) {
        this.taskRepository = taskRepository;
        this.taskSchedulingRepository = taskSchedulingRepository;
        this.taskStatusLogRepository = taskStatusLogRepository;
    }

    public Task createTask(Task task) {

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

        // TODO create temporary object to be persisted and return immediately

        /* TODO: make persistence non-blocking by invoking thread pooling with the interaction of redis and sql */

        // put to redis

        // put to sql db

        return this.taskRepository.save(task);
    }

    @Transactional
    public Optional<Task> readTaskByUUID(UUID taskUUID) {
        return this.taskRepository.findByUuid((taskUUID));
    }

    /* update task scheduling */
    @Transactional
    public TaskScheduling updateTaskScheduling(UUID taskUUID, TaskScheduling updatedTaskScheduling) {
        Task task = this.taskRepository.findByUuid(taskUUID).orElseThrow();
        TaskScheduling taskScheduling = task.getTaskScheduling();

        // add to TaskStatusLog history of TaskScheduling
        TaskStatusLog taskStatusLog = TaskStatusLog.builder()
                .previousStatus(taskScheduling.getStatus())
                .newStatus(updatedTaskScheduling.getStatus())
                .taskScheduling(taskScheduling)
                .build();

        // update task scheduling
        taskScheduling.setNodeId(updatedTaskScheduling.getNodeId());
        taskScheduling.setApplicationId(updatedTaskScheduling.getApplicationId());
        taskScheduling.setApplicationComponentId(updatedTaskScheduling.getApplicationComponentId());
        taskScheduling.setStatus(updatedTaskScheduling.getStatus());
        taskScheduling.addTaskStatusLog(taskStatusLog);

        // persist and return
        return this.taskSchedulingRepository.save(taskScheduling);
    }

    public List<TaskStatusLog> readAllLogsByTask(UUID taskUUID) {
        return new ArrayList<>();
    }

    public List<TaskStatusLog> readAllTaskStatusLogs(UUID taskUUID) {
        Optional<TaskScheduling> taskScheduling = this.taskSchedulingRepository.findByUuid(taskUUID);
        if (taskScheduling.isEmpty()) {
            return new ArrayList<>();
        }
        return taskScheduling.get().getStatusLogs();
    }
}
