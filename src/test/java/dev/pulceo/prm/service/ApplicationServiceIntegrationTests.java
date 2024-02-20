package dev.pulceo.prm.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import dev.pulceo.prm.exception.ApplicationServiceException;
import dev.pulceo.prm.model.application.Application;
import dev.pulceo.prm.repository.ApplicationRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.WireMockSpring;

import java.util.ArrayList;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ApplicationServiceIntegrationTests {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ApplicationRepository applicationRepository;

    public static WireMockServer wireMockServerForPRM = new WireMockServer(WireMockSpring.options().bindAddress("127.0.0.1").port(7878));

    @BeforeAll
    static void setupClass() throws InterruptedException {
        Thread.sleep(100);
        ApplicationServiceIntegrationTests.wireMockServerForPRM.start();
    }

    @BeforeEach
    public void setup() {
        applicationRepository.deleteAll();
    }

    @AfterAll
    public void tearDown() {
        ApplicationServiceIntegrationTests.wireMockServerForPRM.shutdown();
        // applicationRepository.deleteAll();
    }

    @Test
    public void testCreateApplication() throws ApplicationServiceException {
        // given
        UUID nodeUUID = UUID.fromString("0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3");
        Application application = Application.builder()
                .nodeUUID(nodeUUID)
                .name("test-application")
                .applicationComponents(new ArrayList<>())
                .build();

        // mock link request to pna => done in SimulatedPnaAgent
        ApplicationServiceIntegrationTests.wireMockServerForPRM.stubFor(get(urlEqualTo("/api/v1/nodes/" + nodeUUID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("node/prm-read-node-by-uuid-response.json")));

        // mock request to pna


        // when
        Application createdApplication = applicationService.createApplication(application);

        // then
        assertEquals(application, createdApplication);
    }

    @Test
    public void testCreateApplicationWithOneComponent() throws ApplicationServiceException {
        // given

        // when

        // then
    }
}
