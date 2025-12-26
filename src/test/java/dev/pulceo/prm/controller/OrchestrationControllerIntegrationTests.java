package dev.pulceo.prm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import dev.pulceo.prm.dto.orchestration.CreateNewOrchestrationDTO;
import dev.pulceo.prm.dto.orchestration.OrchestrationContextDTO;
import dev.pulceo.prm.dto.orchestration.OrchestrationStatusDTO;
import dev.pulceo.prm.dto.orchestration.PatchOrchestrationPropertiesDTO;
import dev.pulceo.prm.model.orchestration.OrchestrationStatus;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
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

    private static WireMockServer wireMockServerPRM;
    private static WireMockServer wireMockServerPMS;

    @BeforeAll
    static void setupClass() {
        wireMockServerPRM = new WireMockServer(7878);
        wireMockServerPRM.start();
        wireMockServerPMS = new WireMockServer(7777);
        wireMockServerPMS.start();
    }

    @AfterAll
    static void clean() {
        if (wireMockServerPRM.isRunning()) {
            wireMockServerPRM.stop();
        }
        if (wireMockServerPMS.isRunning()) {
            wireMockServerPMS.stop();
        }
    }

    @BeforeEach
    void setup() throws Exception {
        wireMockServerPRM.resetRequests();
        wireMockServerPMS.resetRequests();

        wireMockServerPRM.stubFor(com.github.tomakehurst.wiremock.client.WireMock.put("/api/v1/orchestration-context")
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(
                                OrchestrationContextDTO.builder()
                                        .service("prm")
                                        .name("test")
                                        .uuid("1b1c6697-cb29-4377-bcf8-9fd61ac6c0f3")
                                        .build()
                        )))
        );

        wireMockServerPMS.stubFor(com.github.tomakehurst.wiremock.client.WireMock.put("/api/v1/orchestration-context")
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(
                                OrchestrationContextDTO.builder()
                                        .service("pms")
                                        .name("test")
                                        .uuid("2a2b7798-db39-4477-bcf8-9fd61ac6c0f4")
                                        .build()
                        )))
        );
    }

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

    @Test
    public void testUpdateOrchestrationStatus_NewToRunningToCompleted() throws Exception {
        String orchestrationId = "default";
        OrchestrationStatusDTO newStatus = OrchestrationStatusDTO.builder().orchestrationStatus(OrchestrationStatus.RUNNING).build();
        mockMvc.perform(put("/api/v1/orchestrations/" + orchestrationId + "/status")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newStatus)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.startTimestamp").isNotEmpty());

        newStatus = OrchestrationStatusDTO.builder().orchestrationStatus(OrchestrationStatus.COMPLETED).build();
        mockMvc.perform(put("/api/v1/orchestrations/" + orchestrationId + "/status")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newStatus)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.endTimestamp").isNotEmpty());
    }

}
