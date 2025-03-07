package dev.pulceo.prm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pulceo.prm.dto.task.CreateNewTaskDTO;
import dev.pulceo.prm.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = { "webclient.scheme=http"})
@AutoConfigureMockMvc
public class TaskControllerIntegrationTests {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() throws InterruptedException {
        Thread.sleep(100);
        this.taskRepository.deleteAll();
    }

    @Test
    public void testCreateTaskWithPayload() throws Exception {
        // given
        HashMap<String,String> payload = new HashMap<>();
        payload.put("key", "value");

        Timestamp timeStampCreated = Timestamp.valueOf(LocalDateTime.now());
        byte[] payloadAsBytes = objectMapper.writeValueAsBytes(payload);

        CreateNewTaskDTO createNewTaskDTO = CreateNewTaskDTO.builder()
                .created(timeStampCreated)
                .payload(payloadAsBytes)
                .build();
        String createNewTaskDTOJson = this.objectMapper.writeValueAsString(createNewTaskDTO);

        // when and then
        this.mockMvc.perform(post("/api/v1/tasks")
                .contentType("application/json")
                .content(createNewTaskDTOJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created").isNotEmpty())
                .andExpect(jsonPath("$.arrived").isNotEmpty())
                .andExpect(jsonPath("$.sizeOfWorkload").value(payloadAsBytes.length))
                .andExpect(jsonPath("$.sizeDuringTransmission").value(payloadAsBytes.length))
                .andExpect(jsonPath("$.deadline").value(100));
    }




}
