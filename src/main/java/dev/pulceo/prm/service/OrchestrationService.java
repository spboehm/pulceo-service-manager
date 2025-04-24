package dev.pulceo.prm.service;

import dev.pulceo.prm.exception.OrchestrationServiceException;
import dev.pulceo.prm.model.orchestration.Orchestration;
import dev.pulceo.prm.model.orchestration.OrchestrationContext;
import dev.pulceo.prm.repository.OrchestrationContextRepository;
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
    private final OrchestrationContextRepository contextRepository;

    @Autowired
    public OrchestrationService(OrchestrationRepository orchestrationRepository, OrchestrationContextRepository contextRepository) {
        this.orchestrationRepository = orchestrationRepository;
        this.contextRepository = contextRepository;
    }

    public Orchestration createOrchestration(Orchestration orchestration) throws OrchestrationServiceException {
        if (this.checkIfNameExists(orchestration.getName())) {
            throw new OrchestrationServiceException(String.format("Orchestration with name=%s already exists!", orchestration.getName()));
        }
        // TODO: if there is an Orchestration running, prevent creating a new one, only NEW and TERMINATED orchestrations are allowed
        this.setOrchestrationInOrchestrationContext(orchestration);
        logger.info("Set Orchestration with uuid={}, name={}, and description={} in current OrchestrationContext...",
                orchestration.getUuid(),
                orchestration.getName(),
                orchestration.getDescription());
        return this.orchestrationRepository.save(orchestration);
    }

    public Optional<Orchestration> readOrchestrationByName(String name) {
        return this.orchestrationRepository.findByName(name);
    }

    public Orchestration readDefaultOrchestration() throws OrchestrationServiceException {
        Optional<Orchestration> defaultOrchestration = this.orchestrationRepository.findByName("default");
        if (defaultOrchestration.isPresent()) {
            return defaultOrchestration.get();
        } else {
            throw new OrchestrationServiceException("Default orchestration not found!");
        }
    }

    private boolean checkIfNameExists(String name) {
        return this.orchestrationRepository.findByName(name).isPresent();
    }

    public void deleteOrchestrationByName(String name) throws OrchestrationServiceException {
        OrchestrationContext context = this.getOrCreateOrchestrationContext();
        // reset orchestration in context to "default" if it is the one being deleted
        if (context.getOrchestration().getName().equals(name)) {
            this.setOrchestrationInOrchestrationContext(this.readDefaultOrchestration());
        }
        this.orchestrationRepository.deleteOrchestrationByName(name);
    }

    /* OrchestrationContext methods */
    public OrchestrationContext getOrCreateOrchestrationContext() throws OrchestrationServiceException {
        Optional<OrchestrationContext> optionalContext = contextRepository.findById(1L);
        if (optionalContext.isPresent()) {
            return optionalContext.get();
        } else {
            OrchestrationContext context = new OrchestrationContext();
            context.setId(1L);
            context.setOrchestration(this.readDefaultOrchestration());
            return contextRepository.save(context);
        }
    }

    public OrchestrationContext setOrchestrationInOrchestrationContext(Orchestration orchestration) throws OrchestrationServiceException {
        OrchestrationContext context = this.getOrCreateOrchestrationContext();
        // TODO: check if there is some running orchestration, if it is the, case throw a OrchestrationServiceException
        context.setOrchestration(orchestration);
        return contextRepository.save(context);
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
            this.logger.info("Default Orchestration with uuid={}, name={}, and description={} successfully created.",
                    createdDefaultOrchestration.getUuid(),
                    createdDefaultOrchestration.getName(),
                    createdDefaultOrchestration.getDescription());
        } else {
            this.logger.info("Default Orchestration with with uuid={}, name={}, and description={} already exists, " +
                            "skipping automatic creation...",
                    orchestration.get().getUuid(),
                    orchestration.get().getName(),
                    orchestration.get().getDescription());
        }

        OrchestrationContext context = this.getOrCreateOrchestrationContext();

        this.logger.info("Current OrchestrationContext has id={}, referencing Orchestration with uuid={}, name={}, description={}, and status={}.",
                context.getId(),
                context.getOrchestration().getUuid(),
                context.getOrchestration().getName(),
                context.getOrchestration().getDescription(),
                context.getOrchestration().getStatus());
    }

}
