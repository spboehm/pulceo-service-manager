package dev.pulceo.prm.controller;

import dev.pulceo.prm.api.exception.PnaApiException;
import dev.pulceo.prm.dto.task.*;
import dev.pulceo.prm.exception.TaskServiceException;
import dev.pulceo.prm.model.task.Task;
import dev.pulceo.prm.model.task.TaskScheduling;
import dev.pulceo.prm.model.task.TaskStatusLog;
import dev.pulceo.prm.service.TaskService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final Logger logger = LoggerFactory.getLogger(TaskController.class);

    private final TaskService taskService;

    @Autowired
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // TODO: Get task list by id

    @PostMapping
    public ResponseEntity<CreateNewTaskResponseDTO> createNewTask(@Valid @RequestBody CreateNewTaskDTO createNewTaskDTO) throws InterruptedException {
        Task task = this.taskService.createTask(Task.fromCreateNewTaskDTO(createNewTaskDTO), createNewTaskDTO.getSchedulingProperties());
        return ResponseEntity.status(201).body(CreateNewTaskResponseDTO.fromTask(task));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDTO> readTaskById(@PathVariable UUID id) {
        Optional<Task> task = this.taskService.readTaskByUUID(id);

        if (task.isEmpty()) {
            return ResponseEntity.status(404).build();
        }
        return ResponseEntity.status(200).body(TaskDTO.fromTask(task.get()));
    }

    @GetMapping("")
    public ResponseEntity<List<TaskDTO>> readAllTasks() {
        List<Task> tasks = this.taskService.readAllTasks();
        List<TaskDTO> taskDTOs = new ArrayList<>();
        for (Task task : tasks) {
            taskDTOs.add(TaskDTO.fromTask(task));
        }
        return ResponseEntity.status(200).body(taskDTOs);
    }

    /* scheduling */
    @PutMapping("/{id}/scheduling")
    ResponseEntity<TaskSchedulingDTO> updateTaskScheduling(@PathVariable UUID id, @Valid @RequestBody TaskSchedulingDTO taskSchedulingDTO) {
        Optional<Task> task = this.taskService.readTaskByUUID(id);
        if (task.isEmpty()) {
            return ResponseEntity.status(404).build();
        }
        try {
            logger.info("Scheduling task with status %s".formatted(taskSchedulingDTO.toString()));
            TaskScheduling updatedTaskScheduling = this.taskService.updateTaskScheduling(id, TaskScheduling.fromTaskSchedulingDTO(taskSchedulingDTO));
            this.taskService.queueForScheduling(updatedTaskScheduling.getUuid().toString());
//            this.taskService.enqueue(updatedTaskScheduling.getUuid().toString());
            return ResponseEntity.status(200).body(TaskSchedulingDTO.from(updatedTaskScheduling));
        } catch (TaskServiceException e) {
            return ResponseEntity.status(400).build();
        } catch (PnaApiException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/{id}/scheduling")
    ResponseEntity<TaskSchedulingDTO> readTaskSchedulingByTaskId(@PathVariable UUID id) throws TaskServiceException {
        Optional<TaskScheduling> taskScheduling = this.taskService.readTaskSchedulingByTaskId(String.valueOf(id));
        if (taskScheduling.isEmpty()) {
            return ResponseEntity.status(404).build();
        }
        return ResponseEntity.status(200).body(TaskSchedulingDTO.from(taskScheduling.get()));
    }

    /* logs */
    @GetMapping("/{id}/scheduling/logs")
    public ResponseEntity<List<TaskStatusLogDTO>> readsTaskSchedulingLogs(@PathVariable UUID id) {
        Optional<Task> task = this.taskService.readTaskByUUID(id);
        if (task.isEmpty()) {
            return ResponseEntity.status(404).build();
        }
        List<TaskStatusLog> taskStatusLogs = this.taskService.readAllTaskStatusLogsByTaskId(task.get().getId());
        List<TaskStatusLogDTO> taskStatusLogDTOs = new ArrayList<>();
        for (TaskStatusLog taskStatusLog : taskStatusLogs) {
            taskStatusLogDTOs.add(TaskStatusLogDTO.from(taskStatusLog));
        }
        return ResponseEntity.status(200).body(taskStatusLogDTOs);
    }

    // Exception Handler
    @ExceptionHandler(value = TaskServiceException.class)
    public ResponseEntity<CustomErrorResponse> handleTaskServiceException(TaskServiceException taskServiceException) {
        CustomErrorResponse error = new CustomErrorResponse("BAD_REQUEST", taskServiceException.getMessage());
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setErrorMsg(taskServiceException.getMessage());
        error.setTimestamp(LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

}
