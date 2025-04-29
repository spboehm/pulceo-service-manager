package dev.pulceo.prm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pulceo.prm.exception.OrchestrationServiceException;
import dev.pulceo.prm.model.orchestration.Orchestration;
import dev.pulceo.prm.model.orchestration.OrchestrationContext;
import dev.pulceo.prm.model.orchestration.OrchestrationStatus;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

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

        // when and then
        assertThrows(OrchestrationServiceException.class, () -> {
            this.orchestrationService.createOrchestration(orchestration);
        });
    }

    @Test
    public void testReadDefaultOrchestration() {
        // given
        // orchestrationService.initDefaultOrchestration(); is supposed to automatically create an Orchestration with the name "default"
        String orchestrationName = "default";

        // when
        Optional<Orchestration> orchestration = this.orchestrationService.readOrchestrationWithPropertiesByName(orchestrationName);

        // then
        assertTrue(orchestration.isPresent());
        assertEquals(orchestrationName, orchestration.get().getName());
        assertEquals("default", orchestration.get().getDescription());
        assertEquals(Map.of(), orchestration.get().getProperties());
    }

    @Test
    public void testUpdateOrchestrationStatus() throws OrchestrationServiceException {
        // given
        String orchestrationName = "testOrchestration";
        Orchestration orchestration = Orchestration.builder()
                .name(orchestrationName)
                .description("testOrchestrationDescription")
                .properties(Map.of("key1", "value1", "key2", "value2"))
                .status(OrchestrationStatus.NEW)
                .build();
        Orchestration createdOrchestration = this.orchestrationService.createOrchestration(orchestration);
        OrchestrationStatus initialOrchestrationStatus = createdOrchestration.getStatus();

        // when
        OrchestrationStatus updatedOrchestrationStatus = OrchestrationStatus.RUNNING;
        Orchestration updatedOrchestration = this.orchestrationService.updateOrchestrationStatus(createdOrchestration.getName(), updatedOrchestrationStatus);

        // then
        assertEquals(OrchestrationStatus.NEW, initialOrchestrationStatus);
        assertEquals(updatedOrchestrationStatus, updatedOrchestration.getStatus());
    }

    @Test
    public void testUpdateOrchestrationStatusWithInvalidTransitions() throws OrchestrationServiceException {
        // given
        Orchestration orchestration = this.orchestrationService.readDefaultOrchestration();

        // when and then
        this.orchestrationService.updateOrchestrationStatus(orchestration.getName(), OrchestrationStatus.RUNNING);
        // invalid transition from RUNNING to NEW
        assertThrows(OrchestrationServiceException.class, () -> {
            this.orchestrationService.updateOrchestrationStatus(orchestration.getName(), OrchestrationStatus.NEW);
        });

        this.orchestrationService.updateOrchestrationStatus(orchestration.getName(), OrchestrationStatus.COMPLETED);
        // invalid transition from COMPLETED to RUNNING
        assertThrows(OrchestrationServiceException.class, () -> {
            this.orchestrationService.updateOrchestrationStatus(orchestration.getName(), OrchestrationStatus.RUNNING);
        });
        // invalid transition from COMPLETED to NEW
        assertThrows(OrchestrationServiceException.class, () -> {
            this.orchestrationService.updateOrchestrationStatus(orchestration.getName(), OrchestrationStatus.NEW);
        });
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
        Optional<Orchestration> deletedOrchestration = this.orchestrationService.readOrchestrationWithPropertiesByName(orchestrationName);
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

    @Test
    public void testSetOrchestrationInOrchestrationContextWithRunningOrchestration() throws OrchestrationServiceException {
        // given
        String orchestrationName = "testOrchestration";
        Orchestration orchestration = Orchestration.builder()
                .name(orchestrationName)
                .description("testOrchestrationDescription")
                .properties(Map.of("key1", "value1", "key2", "value2"))
                .status(OrchestrationStatus.NEW)
                .build();
        // create orchestration
        Orchestration createdOrchestration = this.orchestrationService.createOrchestration(orchestration);

        // assert that the Orchestration is in OrchestrationContext
        OrchestrationContext orchestrationContext = this.orchestrationService.getOrCreateOrchestrationContext();
        assertEquals(createdOrchestration, orchestrationContext.getOrchestration());

        // update to running, so that we can test the exception
        this.orchestrationService.updateOrchestrationStatus(createdOrchestration.getName(), OrchestrationStatus.RUNNING);

        // when and then
        // try to set the orchestration in the context again, should throw an OrchestrationException because the current orchestration is already running
        assertThrows(OrchestrationServiceException.class, () -> {
            this.orchestrationService.setOrchestrationInOrchestrationContext(this.orchestrationService.readDefaultOrchestration());
        });

        // assert that the created Orchestration is still in the OrchestrationContext
        assertEquals(createdOrchestration, orchestrationContext.getOrchestration());
    }


}
