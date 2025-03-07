package dev.pulceo.prm.controller;

import dev.pulceo.prm.dto.task.CreateNewTaskDTO;
import dev.pulceo.prm.dto.task.CreateNewTaskResponseDTO;
import dev.pulceo.prm.dto.task.TaskDTO;
import dev.pulceo.prm.model.task.Task;
import dev.pulceo.prm.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

}
