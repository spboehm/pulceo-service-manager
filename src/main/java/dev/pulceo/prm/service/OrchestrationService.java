package dev.pulceo.prm.service;

import dev.pulceo.prm.exception.OrchestrationServiceException;
import dev.pulceo.prm.model.orchestration.Orchestration;
import dev.pulceo.prm.repository.OrchestrationRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OrchestrationService {

    private final Logger logger = LoggerFactory.getLogger(OrchestrationService.class);
    private final OrchestrationRepository orchestrationRepository;

    @Autowired
    public OrchestrationService(OrchestrationRepository orchestrationRepository) {
        this.orchestrationRepository = orchestrationRepository;
    }

    public Orchestration createOrchestration(Orchestration orchestration) {
        return this.orchestrationRepository.save(orchestration);
    }

    public Optional<Orchestration> readOrchestrationByName(String name) {
        return this.orchestrationRepository.findByName(name);
    }

    @PostConstruct
    public void initDefaultOrchestration() throws OrchestrationServiceException {
        Optional<Orchestration> orchestration = this.readOrchestrationByName("default");

        if (orchestration.isEmpty()) {
            Orchestration defaultOrchestration = Orchestration.builder()
                    .name("default")
                    .description("default")
                    .build();
            Orchestration createdDefaultOrchestration = this.createOrchestration(defaultOrchestration);
            this.logger.info("Default Orchestration with uuid {}, name {}, and description {} successfully created",
                    createdDefaultOrchestration.getUuid(),
                    createdDefaultOrchestration.getName(),
                    createdDefaultOrchestration.getDescription());
        } else {
            this.logger.error("Orchestration with name {} already exists.", orchestration.get().getName());
            throw new OrchestrationServiceException("Orchestration with name " + orchestration.get().getName() + " already exists");
        }
    }

}
