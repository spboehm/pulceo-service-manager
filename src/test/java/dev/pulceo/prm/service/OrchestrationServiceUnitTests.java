package dev.pulceo.prm.service;

import dev.pulceo.prm.api.PmsApi;
import dev.pulceo.prm.api.PrmApi;
import dev.pulceo.prm.api.PsmApi;
import dev.pulceo.prm.exception.OrchestrationServiceException;
import dev.pulceo.prm.model.orchestration.Orchestration;
import dev.pulceo.prm.model.orchestration.OrchestrationContext;
import dev.pulceo.prm.repository.OrchestrationContextRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrchestrationServiceUnitTests {

    @Mock
    private PrmApi prmApi;

    @Mock
    private PsmApi psmApi;

    @Mock
    private PmsApi pmsApi;

    @Mock
    private OrchestrationContextRepository orchestrationContextRepository;

    @InjectMocks
    private OrchestrationService orchestrationService;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(orchestrationService, "psmDataDir", "/tmp/psm-data");
    }

    @Test
    public void testCollectStaticOrchestrationData() throws OrchestrationServiceException, IOException {
        // given
        when(this.prmApi.getAllProvidersRaw()).thenReturn(this.readFileToBytes("src/test/resources/__files/api/prmapi-get-all-providers.json"));
        when(this.prmApi.getAllNodesRaw()).thenReturn(this.readFileToBytes("src/test/resources/__files/api/prmapi-get-all-nodes.json"));
        when(this.prmApi.getAllLinksRaw()).thenReturn(this.readFileToBytes("src/test/resources/__files/api/prmapi-get-all-links.json"));
        when(this.prmApi.getAllCpusRaw()).thenReturn(this.readFileToBytes("src/test/resources/__files/api/prmapi-get-all-cpus.json"));
        when(this.prmApi.getAllMemoryRaw()).thenReturn(this.readFileToBytes("src/test/resources/__files/api/prmapi-get-all-memory.json"));
        when(this.prmApi.getAllStorageRaw()).thenReturn(this.readFileToBytes("src/test/resources/__files/api/prmapi-get-all-storage.json"));
        when(this.psmApi.getAllApplicationsRaw()).thenReturn(this.readFileToBytes("src/test/resources/__files/api/psmapi-get-all-applications.json"));
        when(this.pmsApi.getAllMetricRequestsRaw()).thenReturn(this.readFileToBytes("src/test/resources/__files/api/pmsapi-get-all-metric-requests.json"));
        when(this.orchestrationContextRepository.findById(1L)).thenReturn(Optional.of(
                OrchestrationContext.builder()
                        .id(1L)
                        .orchestration(
                                Orchestration.builder()
                                        .name("default")
                                        .description("default")
                                        .properties(Map.of("key1", "value1", "key2", "value2"))
                                        .build())
                        .build()));

        OrchestrationContext orchestrationContext = this.orchestrationContextRepository.findById(1L).orElseThrow();
        UUID orchestrationUuid = orchestrationContext.getOrchestration().getUuid();

        // when
        this.orchestrationService.collectStaticOrchestrationData(orchestrationUuid);

        // then
        Path basePath = Path.of("/tmp/psm-data/raw", orchestrationUuid.toString());
        assertTrue(Files.exists(basePath.resolve("PROVIDERS.json")));
        assertTrue(Files.exists(basePath.resolve("NODES.json")));
        assertTrue(Files.exists(basePath.resolve("LINKS.json")));
        assertTrue(Files.exists(basePath.resolve("CPUS.json")));
        assertTrue(Files.exists(basePath.resolve("MEMORY.json")));
        assertTrue(Files.exists(basePath.resolve("STORAGE.json")));
        assertTrue(Files.exists(basePath.resolve("APPLICATIONS.json")));
        assertTrue(Files.exists(basePath.resolve("METRICS_REQUESTS.json")));
    }

    @Test
    public void testCollectDynamicOrchestrationData() throws OrchestrationServiceException, IOException {
        // given
        when(this.orchestrationContextRepository.findById(1L)).thenReturn(Optional.of(
                OrchestrationContext.builder()
                        .id(1L)
                        .orchestration(
                                Orchestration.builder()
                                        .name("default")
                                        .description("default")
                                        .properties(Map.of("key1", "value1", "key2", "value2"))
                                        .build())
                        .build()));
        //when(this.pmsApi.requestAllCpuUtilizationRaw()).thenReturn(this.readFileToBytes("src/test/resources/__files/api/pmsapi-get-all-cpu-utilization.csv"));

        OrchestrationContext orchestrationContext = this.orchestrationContextRepository.findById(1L).orElseThrow();
        UUID orchestrationUuid = orchestrationContext.getOrchestration().getUuid();

        // when
        this.orchestrationService.collectDynamicOrchestrationData(orchestrationUuid);

        // then
        Path basePath = Path.of("/tmp/psm-data/raw", orchestrationUuid.toString());
        assertTrue(Files.exists(basePath.resolve("CPU_UTIL.csv")));

    }

    private byte[] readFileToBytes(String filePath) throws IOException {
        return Files.readAllBytes(Path.of(filePath));
    }
}
