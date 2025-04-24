package dev.pulceo.prm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pulceo.prm.dto.orchestration.CreateNewOrchestrationDTO;
import dev.pulceo.prm.dto.orchestration.PatchOrchestrationPropertiesDTO;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

    @Test
    public void testReadDefaultOrchestration() throws Exception {
        // given
        // Orchestration with name default is automatically created during application startup
        String orchestrationName = "default";

        // when and then
        this.mockMvc.perform(get("/api/v1/orchestrations/" + orchestrationName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(orchestrationName));
    }

    @Test
    public void testCreateAndReadOrchestrationById() throws Exception {
        // given
        String orchestrationName = "testOrchestration";
        CreateNewOrchestrationDTO createNewOrchestrationDTO = CreateNewOrchestrationDTO.builder()
                .name(orchestrationName)
                .properties(Map.of("key1", "value1", "key2", "value2"))
                .build();

        this.mockMvc.perform(post("/api/v1/orchestrations")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createNewOrchestrationDTO)))
                .andExpect(status().isCreated());

        // when and then
        this.mockMvc.perform(get("/api/v1/orchestrations/" + orchestrationName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(orchestrationName))
                .andExpect(jsonPath("$.properties.key1").value("value1"))
                .andExpect(jsonPath("$.properties.key2").value("value2"));
    }

    @Test
    public void testUpdateOrchestrationProperties() throws Exception {
        // given
        String orchestrationName = "default";
        PatchOrchestrationPropertiesDTO patchOrchestrationPropertiesDTO = PatchOrchestrationPropertiesDTO.builder()
                .properties(Map.of("key1", "newValue1", "key2", "newValue2"))
                .build();

        // when and then
        this.mockMvc.perform(patch("/api/v1/orchestrations/" + orchestrationName + "/properties")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(patchOrchestrationPropertiesDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.properties.key1").value("newValue1"))
                .andExpect(jsonPath("$.properties.key2").value("newValue2"));
    }


}
