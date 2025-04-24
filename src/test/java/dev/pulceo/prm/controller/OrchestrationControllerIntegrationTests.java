package dev.pulceo.prm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pulceo.prm.dto.orchestration.CreateNewOrchestrationDTO;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"webclient.scheme=http"})
@AutoConfigureMockMvc
@Transactional
public class OrchestrationControllerIntegrationTests {

    @Autowired
    private OrchestrationController orchestrationController;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testCreateNewOrchestration() throws Exception {
        // given
        String orchestrationName = "testOrchestration";
        CreateNewOrchestrationDTO createNewOrchestrationDTO = CreateNewOrchestrationDTO.builder()
                .name(orchestrationName)
                .build();

        // when and then
        this.mockMvc.perform(post("/api/v1/orchestrations")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createNewOrchestrationDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(orchestrationName));
    }

    
}
