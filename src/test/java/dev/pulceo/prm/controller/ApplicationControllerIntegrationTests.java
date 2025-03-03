package dev.pulceo.prm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import dev.pulceo.prm.dto.application.CreateNewApplicationDTO;
import dev.pulceo.prm.repository.ApplicationRepository;
import dev.pulceo.prm.service.ApplicationServiceIntegrationTests;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = { "webclient.scheme=http"})
@AutoConfigureMockMvc
public class ApplicationControllerIntegrationTests {

    @Autowired
    private ApplicationController applicationController;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() throws InterruptedException {
        Thread.sleep(100);
        this.applicationRepository.deleteAll();
    }

    @BeforeAll
    static void setupClass() {
        ApplicationServiceIntegrationTests.wireMockServerForPRM.start();
        ApplicationServiceIntegrationTests.wireMockServerForPNA.start();
    }

    @AfterAll
    static void clean() {
        ApplicationServiceIntegrationTests.wireMockServerForPRM.shutdown();
        ApplicationServiceIntegrationTests.wireMockServerForPNA.shutdown();
    }

    @Test
    public void testCreateNewApplicationWithoutApplicationComponents() throws Exception {
        // given
        UUID nodeUUID = UUID.fromString("0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3");
        CreateNewApplicationDTO createNewApplicationDTO = CreateNewApplicationDTO.builder()
                .nodeId(String.valueOf(nodeUUID))
                .name("app-nginx")
                .applicationComponents(new ArrayList<>())
                .build();
        String createNewApplicationDTOJson = this.objectMapper.writeValueAsString(createNewApplicationDTO);

        // when
        // mock link request to pna => done in SimulatedPnaAgent
        ApplicationServiceIntegrationTests.wireMockServerForPRM.stubFor(WireMock.get(urlEqualTo("/api/v1/nodes/" + nodeUUID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("node/prm-read-node-by-uuid-response.json")));

        // mock request to pna
        ApplicationServiceIntegrationTests.wireMockServerForPNA.stubFor(WireMock.post(urlEqualTo("/api/v1/applications"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("application/pna-create-application-without-application-components-response.json")));

        // mock metric request to prm (pna-token)
        ApplicationServiceIntegrationTests.wireMockServerForPRM.stubFor(WireMock.get(urlEqualTo("/api/v1/nodes/0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3/pna-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));

        // when and then
        this.mockMvc.perform(post("/api/v1/applications")
                        .contentType("application/json")
                        .content(createNewApplicationDTOJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nodeId").value(nodeUUID.toString()))
                .andExpect(jsonPath("$.name").value("127.0.0.1-app-nginx"))
                .andExpect(jsonPath("$.applicationComponents").isEmpty());

    }

    @Test
    public void testDeleteApplicationIfExists() throws Exception {
        // given
        UUID nodeUUID = UUID.fromString("0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3");
        CreateNewApplicationDTO createNewApplicationDTO = CreateNewApplicationDTO.builder()
                .nodeId(String.valueOf(nodeUUID))
                .name("app-nginx")
                .applicationComponents(new ArrayList<>())
                .build();
        String createNewApplicationDTOJson = this.objectMapper.writeValueAsString(createNewApplicationDTO);

        // when
        // mock link request to pna => done in SimulatedPnaAgent
        ApplicationServiceIntegrationTests.wireMockServerForPRM.stubFor(WireMock.get(urlEqualTo("/api/v1/nodes/" + nodeUUID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("node/prm-read-node-by-uuid-response.json")));

        // mock request to pna
        ApplicationServiceIntegrationTests.wireMockServerForPNA.stubFor(WireMock.post(urlEqualTo("/api/v1/applications"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("application/pna-create-application-without-application-components-response.json")));

        // mock delete request to pna
        ApplicationServiceIntegrationTests.wireMockServerForPNA.stubFor(WireMock.delete(urlEqualTo("/api/v1/applications/66ae631b-2dff-4334-b0fb-176e054ccbaa"))
                .willReturn(aResponse()
                        .withStatus(204)
                        .withHeader("Content-Type", "application/json")));

        // mock metric request to prm (pna-token)
        ApplicationServiceIntegrationTests.wireMockServerForPRM.stubFor(WireMock.get(urlEqualTo("/api/v1/nodes/0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3/pna-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));

        MvcResult mvcResult = this.mockMvc.perform(post("/api/v1/applications")
                        .contentType("application/json")
                        .content(createNewApplicationDTOJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nodeId").value(nodeUUID.toString()))
                .andExpect(jsonPath("$.name").value("127.0.0.1-app-nginx"))
                .andExpect(jsonPath("$.applicationComponents").isEmpty())
                .andReturn();
        String applicationUUID = objectMapper.readTree(mvcResult.getResponse().getContentAsString()).get("applicationUUID").asText();

        // when and then
        this.mockMvc.perform(delete("/api/v1/applications/" + applicationUUID))
                .andExpect(status().isNoContent());
    }

}
