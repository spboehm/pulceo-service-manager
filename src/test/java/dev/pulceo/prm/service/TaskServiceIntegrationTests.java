package dev.pulceo.prm.service;

import dev.pulceo.prm.model.task.Task;
import dev.pulceo.prm.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = { "webclient.scheme=http"})
public class TaskServiceIntegrationTests {


    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    public void setup() {
        taskRepository.deleteAll();
    }

    @Test
    public void testCreateTask() {
        // given
        Task task = Task.builder().build();

        // when
        Task createdTask = taskService.createTask(task);

        // then
        assertEquals(task, createdTask);
    }

}
