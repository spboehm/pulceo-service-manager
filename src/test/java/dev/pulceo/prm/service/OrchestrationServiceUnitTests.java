package dev.pulceo.prm.service;

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

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrchestrationServiceUnitTests {

    @Mock
    private PrmApi prmApi;

    @Mock
    private PsmApi psmApi;

    @Mock
    private OrchestrationContextRepository orchestrationContextRepository;

    @Mock
    private ApplicationService applicationService;

    @InjectMocks
    private OrchestrationService orchestrationService;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(orchestrationService, "psmDataDir", "/tmp/psm-data");
    }

    @Test
    public void testCollectAllOrchestrationData() throws OrchestrationServiceException, IOException {
        // given
        when(this.prmApi.getAllNodesRaw()).thenReturn(this.readFileToBytes("src/test/resources/__files/api/prmapi-get-all-nodes.json"));
        when(this.prmApi.getAllLinksRaw()).thenReturn(this.readFileToBytes("src/test/resources/__files/api/prmapi-get-all-links.json"));
        when(this.prmApi.getAllCpusRaw()).thenReturn(this.readFileToBytes("src/test/resources/__files/api/prmapi-get-all-cpus.json"));
        when(this.prmApi.getAllMemoryRaw()).thenReturn(this.readFileToBytes("src/test/resources/__files/api/prmapi-get-all-memory.json"));
        when(this.prmApi.getAllStorageRaw()).thenReturn(this.readFileToBytes("src/test/resources/__files/api/prmapi-get-all-storage.json"));
        when(this.psmApi.getAllApplicationsRaw()).thenReturn(this.readFileToBytes("src/test/resources/__files/api/psmapi-get-all-applications.json"));

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

        // when
        this.orchestrationService.collectAllOrchestrationData();

        // then

    }

    public byte[] readFileToBytes(String filePath) throws IOException {
        return Files.readAllBytes(Path.of(filePath));
    }
}
