package dev.pulceo.prm.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import dev.pulceo.prm.api.exception.PrmApiException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.WireMockSpring;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {"webclient.scheme=http"})
@AutoConfigureMockMvc
public class PrmApiTests {

    @Autowired
    private PrmApi prmApi;

    @Autowired
    private MockMvc mockMvc;

    private static final WireMockServer wireMockServer = new WireMockServer(WireMockSpring.options().bindAddress("127.0.0.1").port(7878));
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void setupClass() throws InterruptedException {
        Thread.sleep(1000);
        wireMockServer.start();
    }

    @AfterAll
    static void clean() {
        wireMockServer.shutdown();
    }

    @Test
    public void testGetPnaTokenByNodeId() throws PrmApiException {
        // given
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


}
