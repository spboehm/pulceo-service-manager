package dev.pulceo.prm.controller;

import dev.pulceo.prm.dto.node.NodeDTO;
import dev.pulceo.prm.dto.task.CreateNewTaskDTO;
import dev.pulceo.prm.dto.task.TaskDTO;
import dev.pulceo.prm.model.task.Task;
import dev.pulceo.prm.service.TaskService;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<TaskDTO> createNewTask(@Valid @RequestBody CreateNewTaskDTO createNewTaskDTO) {
        Task task = this.taskService.createTask(Task.fromCreateNewTaskDTO(createNewTaskDTO));
        return ResponseEntity.status(201).body(TaskDTO.fromTask(task));
    }


}
