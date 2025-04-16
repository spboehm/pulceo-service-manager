package dev.pulceo.prm.service;

import dev.pulceo.prm.model.orchestration.Orchestration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"webclient.scheme=http"})
public class OrchestrationServiceIntegrationTests {

    @Autowired
    private OrchestrationService orchestrationService;

    @Test
    public void testCreateOrchestration() {
        // given
        String expectedOrchestrationName = "testOrchestration";
        Orchestration orchestration = Orchestration.builder()
                .name(expectedOrchestrationName)
                .build();

        // when
        Orchestration actualOrchestration = this.orchestrationService.createOrchestration(orchestration);

        // then
        assertNotNull(actualOrchestration.getUuid());
        assertEquals(expectedOrchestrationName, actualOrchestration.getName());
        assertEquals(orchestration.getDescription(), actualOrchestration.getDescription());
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
    }


}
