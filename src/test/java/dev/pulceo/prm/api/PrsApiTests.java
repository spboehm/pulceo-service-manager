package dev.pulceo.prm.api;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {"webclient.scheme=http"})
class PrsApiTests {

    @Autowired
    private PrsApi prsApi;

    private static WireMockServer wireMockServer;

    @BeforeAll
    static void setupClass() throws InterruptedException {
        wireMockServer = new WireMockServer(8181);
        wireMockServer.start();
        Thread.sleep(1000); // Ensure the server is up before tests run
    }

    @AfterAll
    static void clean() {
        if (wireMockServer != null) {
            wireMockServer.shutdown();
        }
    }

    @BeforeEach
    void setup() {
        wireMockServer.resetRequests();
    }

    @Test
    void checkHealth_givenHealthyService_whenCheckHealth_thenReturnsTrue() {
        // given
        wireMockServer.stubFor(get("/prs/health")
                .willReturn(aResponse()
                        .withStatus(200)
                )
        );

        // when and then
        assertDoesNotThrow(() -> prsApi.checkHealth(), "Health check should not throw an exception");
    }

    @Test
    void checkHealth_givenUnhealthyService_whenCheckHealth_thenReturnsFalse() {
        // given
        wireMockServer.stubFor(get("/prs/health")
                .willReturn(aResponse()
                        .withStatus(500)
                )
        );

        // when and then
        assertThrows(RuntimeException.class, () -> prsApi.checkHealth(), "Health check should throw an exception for unhealthy service");
    }

}
