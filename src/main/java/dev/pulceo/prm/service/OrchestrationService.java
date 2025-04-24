package dev.pulceo.prm.service;

import dev.pulceo.prm.exception.OrchestrationServiceException;
import dev.pulceo.prm.model.orchestration.Orchestration;
import dev.pulceo.prm.model.orchestration.OrchestrationContext;
import dev.pulceo.prm.model.orchestration.OrchestrationStatus;
import dev.pulceo.prm.repository.OrchestrationContextRepository;
import dev.pulceo.prm.repository.OrchestrationRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

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
            throw new OrchestrationServiceException(String.format("Orchestration with name=%s already exists", orchestration.getName()));
        }
        Orchestration savedOrchestration = this.orchestrationRepository.save(orchestration);
        try {
            this.setOrchestrationInOrchestrationContext(savedOrchestration);
        } catch (OrchestrationServiceException e) {
            this.logger.warn("Could not set Orchestration in OrchestrationContext: {}", e.getMessage());
        }
        return savedOrchestration;
    }

    public Optional<Orchestration> readOrchestrationByName(String name) {
        return this.orchestrationRepository.findWithPropertiesByName(name);
    }

    public Optional<Orchestration> readOrchestrationByUUID(UUID uuid) {
        return this.orchestrationRepository.findWithPropertiesByUuid(uuid);
    }

    public Orchestration readDefaultOrchestration() throws OrchestrationServiceException {
        Optional<Orchestration> defaultOrchestration = this.orchestrationRepository.findByName("default");
        if (defaultOrchestration.isPresent()) {
            return defaultOrchestration.get();
        } else {
            throw new OrchestrationServiceException("Default orchestration not found");
        }
    }

    private boolean checkIfNameExists(String name) {
        return this.orchestrationRepository.findByName(name).isPresent();
    }

    public Orchestration updateOrchestrationStatus(String name, OrchestrationStatus newOrchestrationStatus) throws OrchestrationServiceException {
        Optional<Orchestration> optionalOrchestration = this.orchestrationRepository.findByName(name);
        if (optionalOrchestration.isPresent()) {
            Orchestration updatedOrchestration = optionalOrchestration.get();
            OrchestrationStatus currentOrchestrationStatus = updatedOrchestration.getStatus();
            try {
                validateOrchestrationStatusTransition(currentOrchestrationStatus, newOrchestrationStatus);
            } catch (OrchestrationServiceException e) {
                this.logger.error("Invalid status transition for Orchestration with uuid={}, name={} from {} to {}: {}",
                        optionalOrchestration.get().getUuid(), name, currentOrchestrationStatus, newOrchestrationStatus, e.getMessage());
                throw new OrchestrationServiceException(e);
            }
            updatedOrchestration.setStatus(newOrchestrationStatus);
            this.logger.info("Updating Orchestration with uuid={}, name={} from status={} to status={}", updatedOrchestration.getUuid(), name, currentOrchestrationStatus, updatedOrchestration.getStatus());
            return updatedOrchestration;
        } else {
            this.logger.error("Orchestration with name={} not found!", name);
            throw new OrchestrationServiceException("Orchestration with name=%s not found".formatted(name));
        }
    }

    private void validateOrchestrationStatusTransition(OrchestrationStatus currentStatus, OrchestrationStatus newStatus) throws OrchestrationServiceException {
        if ((currentStatus == OrchestrationStatus.NEW && newStatus == OrchestrationStatus.RUNNING) ||
                (currentStatus == OrchestrationStatus.RUNNING && newStatus == OrchestrationStatus.COMPLETED)) {
            return; // Valid transition
        }
        throw new OrchestrationServiceException(String.format("Invalid status transition from %s to %s", currentStatus, newStatus));
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
        // only automatically set the orchestration in the context if the currently referenced orchestration is not RUNNING
        if (context.getOrchestration().getStatus() == OrchestrationStatus.NEW || context.getOrchestration().getStatus() == OrchestrationStatus.COMPLETED) {
            this.logger.info("Set Orchestration with uuid={}, name={}, description={}, and status={} in OrchestrationContext",
                    orchestration.getUuid(),
                    orchestration.getName(),
                    orchestration.getDescription(),
                    orchestration.getStatus());
            context.setOrchestration(orchestration);
            return contextRepository.save(context);
        } else {
            this.logger.error("OrchestrationContext is already referencing an Orchestration with uuid={}, name={}, and status={}, not updating it",
                    context.getOrchestration().getUuid(),
                    context.getOrchestration().getName(),
                    context.getOrchestration().getStatus());
            throw new OrchestrationServiceException("Could not set Orchestration in OrchestrationContext, " +
                    "current OrchestrationContext is already referencing an Orchestration with uuid=%s, name=%s, and status=%s".formatted(
                            context.getOrchestration().getUuid(),
                            context.getOrchestration().getName(),
                            context.getOrchestration().getStatus()));
        }
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
            this.logger.info("Default Orchestration with uuid={}, name={}, description={}, and status={} successfully created",
                    createdDefaultOrchestration.getUuid(),
                    createdDefaultOrchestration.getName(),
                    createdDefaultOrchestration.getDescription(),
                    createdDefaultOrchestration.getStatus());
        } else {
            this.logger.info("Default Orchestration with with uuid={}, name={}, description={}, and status={} already exists, " +
                            "skipping automatic creation",
                    orchestration.get().getUuid(),
                    orchestration.get().getName(),
                    orchestration.get().getDescription(),
                    orchestration.get().getStatus());
        }

        OrchestrationContext context = this.getOrCreateOrchestrationContext();

        this.logger.info("Current OrchestrationContext has id={}, referencing Orchestration with uuid={}, name={}, description={}, and status={}",
                context.getId(),
                context.getOrchestration().getUuid(),
                context.getOrchestration().getName(),
                context.getOrchestration().getDescription(),
                context.getOrchestration().getStatus());
    }


}
