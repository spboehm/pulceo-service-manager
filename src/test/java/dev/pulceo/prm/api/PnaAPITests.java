package dev.pulceo.prm.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import dev.pulceo.prm.api.dto.task.CreateNewTaskOnPnaDTO;
import dev.pulceo.prm.api.dto.task.CreateNewTaskOnPnaResponseDTO;
import dev.pulceo.prm.api.exception.PnaApiException;
import dev.pulceo.prm.model.task.TaskStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.WireMockSpring;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {"webclient.scheme=http"})
public class PnaAPITests {

    @Autowired
    private PnaApi pnaApi;

    public static WireMockServer wireMockServerForPRM = new WireMockServer(WireMockSpring.options().bindAddress("127.0.0.1").port(7878));
    public static WireMockServer wireMockServerForPNA = new WireMockServer(WireMockSpring.options().bindAddress("127.0.0.1").port(7676));

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void setupClass() throws InterruptedException {
        Thread.sleep(1000);
        wireMockServerForPRM.start();
        wireMockServerForPNA.start();
    }

    @BeforeEach
    public void setup() {
        wireMockServerForPRM.resetRequests();
        wireMockServerForPNA.resetRequests();
    }

    @AfterAll
    static void clean() {
        wireMockServerForPRM.shutdown();
        wireMockServerForPNA.shutdown();
    }

    @Test
    public void testCreateNewTaskOnPna() throws PnaApiException {
        // given
        String nodeId = "0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3";

        CreateNewTaskOnPnaDTO createNewTaskOnPnaDTO = CreateNewTaskOnPnaDTO.builder()
                .applicationId("edf1b3b3-0b1c-4b3b-8b3b-3b3b3b3b3b3b")
                .applicationComponentId("edf1b3b3-0b1c-4b3b-8b3b-3b3b3b3b3b3b")
                .payload(new byte[0])
                .callbackProtocol("mqtt")
                .callbackEndpoint("callbacks/edf1b3b3-0b1c-4b3b-8b3b-3b3b3b3b3b3b")
                .destinationApplicationComponentProtocol("http")
                .destinationApplicationComponentEndpoint("/api/v1/workload")
                .build();

        CreateNewTaskOnPnaResponseDTO expectedCreateNewTaskOnPnaResponseDTO = CreateNewTaskOnPnaResponseDTO
                .builder()
                .globalTaskUUID("123e4567-e89b-12d3-a456-426614174000")
                .remoteNodeUUID(UUID.fromString("8f08e447-7ccd-4657-a873-a1d43a733b1a"))
                .remoteTaskUUID(UUID.fromString("123e4567-e89b-12d3-a456-426614174001"))
                .status(TaskStatus.NEW)
                .build();

        // when
        wireMockServerForPRM.stubFor(get(urlEqualTo("/api/v1/nodes/" + nodeId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("node/prm-read-node-by-uuid-response.json")));

        wireMockServerForPRM.stubFor(get(urlEqualTo("/api/v1/nodes/" + nodeId + "/pna-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("b0hRUGwxT0hNYnhGbGoyQ2tlQnBGblAxOmdHUHM3MGtRRWNsZVFMSmdZclFhVUExb0VpNktGZ296")));

        wireMockServerForPNA.stubFor(post(urlEqualTo("/api/v1/tasks"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("task/pna-create-task-response.json")));

        CreateNewTaskOnPnaResponseDTO createNewTaskOnPnaResponseDTO = pnaApi.createNewTaskOnPna(nodeId, createNewTaskOnPnaDTO);

        // then
        assertEquals(expectedCreateNewTaskOnPnaResponseDTO, createNewTaskOnPnaResponseDTO);
        wireMockServerForPNA.verify(postRequestedFor(urlEqualTo("/api/v1/tasks")));
    }

}
