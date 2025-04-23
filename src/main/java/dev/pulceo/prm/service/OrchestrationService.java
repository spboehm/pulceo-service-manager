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

    public Orchestration createOrchestration(Orchestration orchestration) throws OrchestrationServiceException {
        if (this.checkIfNameExists(orchestration.getName())) {
            throw new OrchestrationServiceException(String.format("Orchestration with name %s already exists!", orchestration.getName()));
        }
        return this.orchestrationRepository.save(orchestration);
    }

    public Optional<Orchestration> readOrchestrationByName(String name) {
        return this.orchestrationRepository.findByName(name);
    }

    private boolean checkIfNameExists(String name) {
        return this.orchestrationRepository.findByName(name).isPresent();
    }

    public void deleteOrchestrationByName(String name) {
        this.orchestrationRepository.deleteOrchestrationByName(name);
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
            this.logger.info("Default Orchestration with uuid {}, name {}, and description {} successfully created.",
                    createdDefaultOrchestration.getUuid(),
                    createdDefaultOrchestration.getName(),
                    createdDefaultOrchestration.getDescription());
        } else {
            this.logger.info("Default Orchestration with with uuid {}, name {}, and description {} already exists, " +
                            "skipping automatic creation...",
                    orchestration.get().getUuid(),
                    orchestration.get().getName(),
                    orchestration.get().getDescription());
        }
    }

}
