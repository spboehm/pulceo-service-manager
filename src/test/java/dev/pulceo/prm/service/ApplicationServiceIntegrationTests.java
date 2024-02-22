package dev.pulceo.prm.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import dev.pulceo.prm.exception.ApplicationServiceException;
import dev.pulceo.prm.model.application.Application;
import dev.pulceo.prm.model.application.ApplicationComponent;
import dev.pulceo.prm.model.application.ApplicationComponentProtocol;
import dev.pulceo.prm.model.application.ApplicationComponentType;
import dev.pulceo.prm.repository.ApplicationRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.WireMockSpring;

import java.util.ArrayList;
import java.util.Map;
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
    public static WireMockServer wireMockServerForPNA = new WireMockServer(WireMockSpring.options().bindAddress("127.0.0.1").port(7676));


    @BeforeAll
    static void setupClass() throws InterruptedException {
        Thread.sleep(100);
        ApplicationServiceIntegrationTests.wireMockServerForPRM.start();
        ApplicationServiceIntegrationTests.wireMockServerForPNA.start();

    }

    @BeforeEach
    public void setup() {
        applicationRepository.deleteAll();
    }

    @AfterAll
    public void tearDown() {
        ApplicationServiceIntegrationTests.wireMockServerForPRM.shutdown();
        ApplicationServiceIntegrationTests.wireMockServerForPNA.shutdown();
        // applicationRepository.deleteAll();
    }

    @Test
    public void testCreateApplication() throws ApplicationServiceException {
        // given
        UUID nodeUUID = UUID.fromString("0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3");
        Application application = Application.builder()
                .remoteApplicationUUID(UUID.fromString("66ae631b-2dff-4334-b0fb-176e054ccbaa"))
                .nodeUUID(nodeUUID)
                .name("app-nginx")
                .applicationComponents(new ArrayList<>())
                .build();

        // mock link request to pna => done in SimulatedPnaAgent
        ApplicationServiceIntegrationTests.wireMockServerForPRM.stubFor(get(urlEqualTo("/api/v1/nodes/" + nodeUUID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("node/prm-read-node-by-uuid-response.json")));

        // mock request to pna
        ApplicationServiceIntegrationTests.wireMockServerForPNA.stubFor(post(urlEqualTo("/api/v1/applications"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("application/pna-create-application-without-application-components-response.json")));

        // when
        Application createdApplication = applicationService.createApplication(application);

        // then
        assertEquals(application, createdApplication);
    }

    @Test
    public void testCreateApplicationWithOneComponent() throws ApplicationServiceException {
        // given
        UUID nodeUUID = UUID.fromString("0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3");
        Application application = Application.builder()
                .remoteApplicationUUID(UUID.fromString("66ae631b-2dff-4334-b0fb-176e054ccbaa"))
                .nodeUUID(nodeUUID)
                .name("app-nginx")
                .build();

        ApplicationComponent applicationComponent = ApplicationComponent.builder()
                .remoteApplicationComponentUUID(UUID.fromString("66ae631b-2dff-4334-b0fb-176e054ccbcc"))
                .nodeUUID(nodeUUID)
                .nodeHost("127.0.0.1")
                .name("component-nginx")
                .image("nginx")
                .protocol(String.valueOf(ApplicationComponentProtocol.HTTP))
                .port(80)
                .application(application)
                .applicationComponentType(ApplicationComponentType.PUBLIC)
//                .environmentVariables(Map.ofEntries(
//                        Map.entry("TEST", "TEST")
//                ))
                .build();

        application.addApplicationComponent(applicationComponent);

        // mock link request to pna => done in SimulatedPnaAgent
        ApplicationServiceIntegrationTests.wireMockServerForPRM.stubFor(get(urlEqualTo("/api/v1/nodes/" + nodeUUID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("node/prm-read-node-by-uuid-response.json")));

        // mock request to pna
        ApplicationServiceIntegrationTests.wireMockServerForPNA.stubFor(post(urlEqualTo("/api/v1/applications"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("application/pna-create-application-with-one-application-component-response.json")));

        // when
        Application createdApplication = applicationService.createApplication(application);

        // then
        assertEquals(application, createdApplication);
    }
}
