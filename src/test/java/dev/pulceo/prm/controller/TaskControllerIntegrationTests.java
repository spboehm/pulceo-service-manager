package dev.pulceo.prm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pulceo.prm.dto.task.CreateNewTaskDTO;
import dev.pulceo.prm.repository.TaskRepository;
import dev.pulceo.prm.repository.TaskStatusLogRepository;
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

@SpringBootTest(properties = {"webclient.scheme=http"})
@AutoConfigureMockMvc
public class TaskControllerIntegrationTests {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TaskStatusLogRepository taskStatusLogRepository;

    @BeforeEach
    public void setUp() throws InterruptedException {
        Thread.sleep(100);
        this.taskStatusLogRepository.deleteAll();
        this.taskRepository.deleteAll();
    }

    @Test
    public void testCreateTaskWithPayloadAndMetaData() throws Exception {
        // given
        HashMap<String, String> payload = new HashMap<>();
        payload.put("key", "value");

        Timestamp timeStampCreated = Timestamp.valueOf(LocalDateTime.now());
        byte[] payloadAsBytes = objectMapper.writeValueAsBytes(payload);

        HashMap<String, String> requirements = new HashMap<>();
        requirements.put("cpu_mips", "3000");

        HashMap<String, String> properties = new HashMap<>();
        properties.put("priority", "1");

        CreateNewTaskDTO createNewTaskDTO = CreateNewTaskDTO.builder()
                .created(timeStampCreated)
                .payload(payloadAsBytes)
                .requirements(requirements)
                .properties(properties)
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
                .andExpect(jsonPath("$.deadline").value(100))
                .andExpect(jsonPath("$.requirements").isNotEmpty())
                .andExpect(jsonPath("$.requirements.cpu_mips").value("3000"))
                .andExpect(jsonPath("$.properties").isNotEmpty())
                .andExpect(jsonPath("$.properties.priority").value("1"));
    }

    @Test
    public void testReadTaskByUUID() {


    }

}
