package dev.pulceo.prm.controller;

import dev.pulceo.prm.dto.task.CreateNewTaskDTO;
import dev.pulceo.prm.dto.task.CreateNewTaskResponseDTO;
import dev.pulceo.prm.dto.task.TaskDTO;
import dev.pulceo.prm.dto.task.TaskStatusLogDTO;
import dev.pulceo.prm.model.task.Task;
import dev.pulceo.prm.model.task.TaskStatusLog;
import dev.pulceo.prm.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    @Autowired
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // TODO: Get task by id

    // TODO: Get task list by id

    @PostMapping
    public ResponseEntity<CreateNewTaskResponseDTO> createNewTask(@Valid @RequestBody CreateNewTaskDTO createNewTaskDTO) {
        Task task = this.taskService.createTask(Task.fromCreateNewTaskDTO(createNewTaskDTO));
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


    /* scheduling */
    @GetMapping("/{id}/scheduling/logs")
    public ResponseEntity<List<TaskStatusLogDTO>> readTaskScheduling(@PathVariable String id) {
        Optional<Task> task = this.taskService.readTaskByUUID(UUID.fromString(id));
        if (task.isEmpty()) {
            return ResponseEntity.status(404).build();
        }
        List<TaskStatusLog> taskStatusLogs = this.taskService.readTaskStatusLogs(task.get().getUuid());
        List<TaskStatusLogDTO> taskStatusLogDTOs = new ArrayList<>();
        for (TaskStatusLog taskStatusLog : taskStatusLogs) {
            taskStatusLogDTOs.add(TaskStatusLogDTO.from(taskStatusLog));
        }
        return ResponseEntity.status(200).body(taskStatusLogDTOs);
    }

}
