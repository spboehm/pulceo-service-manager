package dev.pulceo.prm.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import dev.pulceo.prm.api.exception.PrmApiException;
import dev.pulceo.prm.dto.node.NodeDTO;
import dev.pulceo.prm.dto.node.NodePropertiesDTO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.WireMockSpring;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {"webclient.scheme=http"})
public class PrmApiTests {

    @Autowired
    private PrmApi prmApi;

    private static final WireMockServer wireMockServer = new WireMockServer(WireMockSpring.options().bindAddress("127.0.0.1").port(7878));
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void setupClass() throws InterruptedException {
        Thread.sleep(1000);
        wireMockServer.start();
    }

    @BeforeEach
    public void setup() {
        wireMockServer.resetRequests();
    }

    @AfterAll
    static void clean() {
        wireMockServer.shutdown();
    }

    @Test
    public void testGetPnaTokenByNodeId() throws PrmApiException {
        // given
        ReflectionTestUtils.setField(prmApi, "pnaTokenCache", new ConcurrentHashMap<>());
        String nodeId = "0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3";
        String expectedPnaToken = "b0hRUGwxT0hNYnhGbGoyQ2tlQnBGblAxOmdHUHM3MGtRRWNsZVFMSmdZclFhVUExb0VpNktGZ296";

        // when
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/nodes/" + nodeId + "/pna-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(expectedPnaToken)));

        String pnaToken = prmApi.getPnaTokenByNodeId(nodeId);

        // then
        assertEquals(expectedPnaToken, pnaToken);
        wireMockServer.verify(getRequestedFor(urlEqualTo("/api/v1/nodes/" + nodeId + "/pna-token")));
    }

    @Test
    public void testGetPnaTokenFromCache() throws PrmApiException {
        // given
        ReflectionTestUtils.setField(prmApi, "pnaTokenCache", new ConcurrentHashMap<>());
        String nodeId = "0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3";
        String expectedPnaToken = "b0hRUGwxT0hNYnhGbGoyQ2tlQnBGblAxOmdHUHM3MGtRRWNsZVFMSmdZclFhVUExb0VpNktGZ296";
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/nodes/" + nodeId + "/pna-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(expectedPnaToken)));
        prmApi.getPnaTokenByNodeId(nodeId);
        wireMockServer.resetRequests();

        // when
        String pnaTokenCached = prmApi.getPnaTokenByNodeId(nodeId);

        // then
        assertEquals(expectedPnaToken, pnaTokenCached);
        wireMockServer.verify(0, getRequestedFor(urlEqualTo("/api/v1/nodes/" + nodeId + "/pna-token")));
    }

    @Test
    public void testGetNodeById() throws PrmApiException {
        // given
        ReflectionTestUtils.setField(prmApi, "nodeCache", new ConcurrentHashMap<>());
        String nodeId = "0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3";

        NodeDTO expectedNodeDTO = NodeDTO.builder()
                .uuid(UUID.fromString(nodeId))
                .providerName("default")
                .hostname("127.0.0.1")
                .pnaUUID(UUID.fromString("0247fea1-3ca3-401b-8fa2-b6f83a469680"))
                .node(NodePropertiesDTO.builder()
                        .name("127.0.0.1")
                        .build())
                .build();

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/nodes/" + nodeId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("node/prm-read-node-by-uuid-response.json")));

        // when
        NodeDTO nodeDTO = this.prmApi.getNodeById(nodeId);

        // then
        assertEquals(expectedNodeDTO, nodeDTO);
        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/api/v1/nodes/" + nodeId)));
    }

    @Test
    public void testGetNodeFromCache() throws PrmApiException {
        // given
        ReflectionTestUtils.setField(prmApi, "nodeCache", new ConcurrentHashMap<>());
        String nodeId = "0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3";
        NodeDTO expectedNodeDTO = NodeDTO.builder()
                .uuid(UUID.fromString(nodeId))
                .providerName("default")
                .hostname("127.0.0.1")
                .pnaUUID(UUID.fromString("0247fea1-3ca3-401b-8fa2-b6f83a469680"))
                .node(NodePropertiesDTO.builder()
                        .name("127.0.0.1")
                        .build())
                .build();
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/nodes/" + nodeId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("node/prm-read-node-by-uuid-response.json")));
        this.prmApi.getNodeById(nodeId);
        wireMockServer.resetRequests();

        // when
        NodeDTO nodeDTO = this.prmApi.getNodeById(nodeId);

        // then
        assertEquals(expectedNodeDTO, nodeDTO);
        wireMockServer.verify(0, getRequestedFor(urlEqualTo("/api/v1/nodes/" + nodeId)));
    }
}
