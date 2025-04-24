package dev.pulceo.prm.service;

import dev.pulceo.prm.exception.OrchestrationServiceException;
import dev.pulceo.prm.model.orchestration.Orchestration;
import dev.pulceo.prm.model.orchestration.OrchestrationContext;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"webclient.scheme=http"})
@Transactional
public class OrchestrationServiceIntegrationTests {

    @Autowired
    private OrchestrationService orchestrationService;

    @Test
    public void testCreateOrchestration() throws OrchestrationServiceException {
        // given
        String expectedOrchestrationName = "testOrchestration";
        Orchestration orchestration = Orchestration.builder()
                .name(expectedOrchestrationName)
                .description("testOrchestrationDescription")
                .properties(Map.of("key1", "value1", "key2", "value2"))
                .build();

        // when
        Orchestration actualOrchestration = this.orchestrationService.createOrchestration(orchestration);

        // then
        assertNotNull(actualOrchestration.getUuid());
        assertEquals(expectedOrchestrationName, actualOrchestration.getName());
        assertEquals(orchestration.getDescription(), actualOrchestration.getDescription());
    }

    @Test
    public void testCreateOrchestrationWithExistingName() throws OrchestrationServiceException {
        // given
        String orchestrationName = "testOrchestration";
        Orchestration orchestration = Orchestration.builder()
                .name(orchestrationName)
                .description("testOrchestrationDescription")
                .properties(Map.of("key1", "value1", "key2", "value2"))
                .build();
        this.orchestrationService.createOrchestration(orchestration);

        // when
        OrchestrationServiceException exception = assertThrows(OrchestrationServiceException.class, () -> {
            this.orchestrationService.createOrchestration(orchestration);
        });

        // then
        assertEquals("Orchestration with name=" + orchestrationName + " already exists!", exception.getMessage());
    }

    @Test
    public void testReadDefaultOrchestration() {
        // given
        // orchestrationService.initDefaultOrchestration(); is supposed to automatically create an Orchestration with the name "default"
        String orchestrationName = "default";

        // when
        Optional<Orchestration> orchestration = this.orchestrationService.readOrchestrationByName(orchestrationName);

        // then
        assertTrue(orchestration.isPresent());
        assertEquals(orchestrationName, orchestration.get().getName());
        assertEquals("default", orchestration.get().getDescription());
        assertEquals(Map.of(), orchestration.get().getProperties());
    }

    @Test
    public void testDeleteOrchestration() throws OrchestrationServiceException {
        // given
        String orchestrationName = "testOrchestration";
        Orchestration orchestration = Orchestration.builder()
                .name(orchestrationName)
                .description("testOrchestrationDescription")
                .properties(Map.of("key1", "value1", "key2", "value2"))
                .build();
        this.orchestrationService.createOrchestration(orchestration);

        // when
        this.orchestrationService.deleteOrchestrationByName(orchestrationName);

        // then
        Optional<Orchestration> deletedOrchestration = this.orchestrationService.readOrchestrationByName(orchestrationName);
        assertFalse(deletedOrchestration.isPresent());
    }

    @Test
    public void testGetOrCreateOrchestrationContextWithDefaultOrchestration() throws OrchestrationServiceException {
        // given
        // orchestrationService.initDefaultOrchestration(); is supposed to automatically create an Orchestration with the name "default"

        // when
        OrchestrationContext orchestrationContext = this.orchestrationService.getOrCreateOrchestrationContext();

        // then
        assertNotNull(orchestrationContext);
        assertEquals(1L, orchestrationContext.getId());
        assertNotNull(orchestrationContext.getOrchestration());
        assertEquals("default", orchestrationContext.getOrchestration().getName());

    }

    @Test
    public void testSetOrchestrationInOrchestrationContext() throws OrchestrationServiceException {
        // given
        String orchestrationName = "testOrchestration";
        Orchestration orchestration = Orchestration.builder()
                .name(orchestrationName)
                .description("testOrchestrationDescription")
                .properties(Map.of("key1", "value1", "key2", "value2"))
                .build();
        this.orchestrationService.createOrchestration(orchestration);

        // when
        OrchestrationContext orchestrationContext = this.orchestrationService.setOrchestrationInOrchestrationContext(orchestration);

        // then
        assertNotNull(orchestrationContext);
        assertEquals(1L, orchestrationContext.getId());
        assertNotNull(orchestrationContext.getOrchestration());
        assertEquals(orchestrationName, orchestrationContext.getOrchestration().getName());
    }

}
