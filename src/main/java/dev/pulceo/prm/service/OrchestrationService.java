package dev.pulceo.prm.service;

import dev.pulceo.prm.api.PmsApi;
import dev.pulceo.prm.api.PnaApi;
import dev.pulceo.prm.api.PrmApi;
import dev.pulceo.prm.api.PsmApi;
import dev.pulceo.prm.api.dto.metricexports.MetricType;
import dev.pulceo.prm.api.exception.PmsApiException;
import dev.pulceo.prm.api.exception.PrmApiException;
import dev.pulceo.prm.api.exception.PsmApiException;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class OrchestrationService {

    private final Logger logger = LoggerFactory.getLogger(OrchestrationService.class);
    private final OrchestrationRepository orchestrationRepository;
    private final OrchestrationContextRepository contextRepository;
    private final PsmApi psmApi;
    private final PrmApi prmApi;
    private final PmsApi pmsApi;
    private final PnaApi pnaApi;
    @Value("${psm.data.dir}")
    private String psmDataDir;
    private final Lock reportCreationLock = new ReentrantLock();

    @Autowired
    public OrchestrationService(OrchestrationRepository orchestrationRepository, OrchestrationContextRepository contextRepository, PsmApi psmApi, PrmApi prmApi, PmsApi pmsApi, PnaApi pnaApi) {
        this.orchestrationRepository = orchestrationRepository;
        this.contextRepository = contextRepository;
        this.psmApi = psmApi;
        this.prmApi = prmApi;
        this.pmsApi = pmsApi;
        this.pnaApi = pnaApi;
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

    public Optional<Orchestration> readOrchestrationWithPropertiesByName(String name) {
        return this.orchestrationRepository.findWithPropertiesByName(name);
    }

    public Optional<Orchestration> readOrchestrationWithPropertiesByUUID(UUID uuid) {
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

    public Orchestration updateOrchestrationStatus(String id, OrchestrationStatus newOrchestrationStatus) throws OrchestrationServiceException {
        Optional<Orchestration> optionalOrchestration = this.resolveOrchestration(id);
        if (optionalOrchestration.isPresent()) {
            Orchestration updatedOrchestration = optionalOrchestration.get();
            OrchestrationStatus currentOrchestrationStatus = updatedOrchestration.getStatus();
            try {
                validateOrchestrationStatusTransition(currentOrchestrationStatus, newOrchestrationStatus);
            } catch (OrchestrationServiceException e) {
                this.logger.error("Invalid status transition for Orchestration with uuid={}, name={} from {} to {}: {}",
                        optionalOrchestration.get().getUuid(), updatedOrchestration.getName(), currentOrchestrationStatus, newOrchestrationStatus, e.getMessage());
                throw new OrchestrationServiceException(e);
            }
            updatedOrchestration.setStatus(newOrchestrationStatus);
            this.logger.info("Updating Orchestration with uuid={}, name={} from status={} to status={}", updatedOrchestration.getUuid(), updatedOrchestration.getName(), currentOrchestrationStatus, updatedOrchestration.getStatus());
            return updatedOrchestration;
        } else {
            this.logger.error("Orchestration with id={} not found!", id);
            throw new OrchestrationServiceException("Orchestration with id=%s not found".formatted(id));
        }
    }

    private void validateOrchestrationStatusTransition(OrchestrationStatus currentStatus, OrchestrationStatus newStatus) throws OrchestrationServiceException {
        if ((currentStatus == OrchestrationStatus.NEW && newStatus == OrchestrationStatus.RUNNING) ||
                (currentStatus == OrchestrationStatus.RUNNING && newStatus == OrchestrationStatus.COMPLETED)) {
            return; // Valid transition
        }
        throw new OrchestrationServiceException(String.format("Invalid status transition from %s to %s", currentStatus, newStatus));
    }

    public Orchestration updateOrchestrationProperties(String id, Map<String, String> properties) throws OrchestrationServiceException {
        Optional<Orchestration> optionalOrchestration = this.resolveOrchestration(id);
        if (optionalOrchestration.isPresent()) {
            Orchestration orchestration = optionalOrchestration.get();
            orchestration.setProperties(properties);
            this.logger.info("Updating Orchestration with uuid={}, name={} properties to {}", orchestration.getUuid(), orchestration.getName(), properties);
            return this.orchestrationRepository.save(orchestration);
        } else {
            this.logger.error("Orchestration with id={} not found", id);
            throw new OrchestrationServiceException("Orchestration with id=%s not found".formatted(id));
        }
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

    private Optional<Orchestration> resolveOrchestration(String id) {
        if (checkIfUUID(id)) {
            return this.readOrchestrationWithPropertiesByUUID(UUID.fromString(id));
        } else {
            return this.readOrchestrationWithPropertiesByName(id);
        }
    }

    private boolean checkIfUUID(String uuid) {
        String uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
        return uuid.matches(uuidRegex);
    }

    public void reset() {
        this.pnaApi.resetAllPna();
        // reset PRM
        this.prmApi.resetOrchestrationContext();
        // reset PSM
        this.pmsApi.resetOrchestrationContext();
        // TODO: inform about new orchestration context
    }

    public void collectDynamicOrchestrationData(UUID orchestrationUuid, boolean cleanUp) throws OrchestrationServiceException {
        if (this.reportCreationLock.tryLock()) {
            try {
                this.logger.info("Collecting dynamic orchestration data for orchestration with uuid={}", orchestrationUuid);
                this.createDirsForOrchestrationData(orchestrationUuid);

                try {
                    for (MetricType metricType : MetricType.values()) {
                        this.pmsApi.requestMetric(orchestrationUuid, metricType, cleanUp);
                    }
                    // TODO: Task Status Logs
                } catch (PmsApiException e) {
                    this.logger.error("Failed to collect dynamic orchestration data", e);
                    throw new OrchestrationServiceException("Failed to collect dynamic orchestration data", e);
                }
                this.logger.info("Collecting dynamic orchestration data for orchestrationUuid={} successfully completed", orchestrationUuid);
            } finally {
                this.reportCreationLock.unlock();
            }
        } else {
            this.logger.warn("Dynamic orchestration data collection is already in progress for orchestration with uuid={}", orchestrationUuid);
            throw new OrchestrationServiceException("Dynamic orchestration data collection is already in progress for orchestration with uuid=%s".formatted(orchestrationUuid));
        }
    }

    public void collectStaticOrchestrationData(UUID orchestrationUuid, boolean cleanUp) throws OrchestrationServiceException {
        if (this.reportCreationLock.tryLock()) {
            try {
                this.createDirsForOrchestrationData(orchestrationUuid);

                this.prmApi.collectStaticOrchestrationData(orchestrationUuid, cleanUp);
                this.psmApi.collectStaticOrchestrationData(orchestrationUuid, cleanUp);
                this.pmsApi.collectStaticOrchestrationData(orchestrationUuid, cleanUp);
            } catch (PrmApiException | PsmApiException | PmsApiException e) {
                this.logger.error("Failed to collect static orchestration data", e);
                throw new OrchestrationServiceException("Failed to collect static orchestration data", e);
            } finally {
                this.reportCreationLock.unlock();
            }
        } else {
            this.logger.warn("Static orchestration data collection is already in progress for orchestration with uuid={}", orchestrationUuid);
            throw new OrchestrationServiceException("Static orchestration data collection is already in progress for orchestration with uuid=%s".formatted(orchestrationUuid));
        }

    }

    private void createDirsForOrchestrationData(UUID orchestrationUUID) {
        logger.info("Creating directories for orchestration data with uuid={}", orchestrationUUID);
        try {
            Files.createDirectories(Path.of(this.psmDataDir, "raw", orchestrationUUID.toString()));
            Files.createDirectories(Path.of(this.psmDataDir, "plots", orchestrationUUID.toString()));
            Files.createDirectories(Path.of(this.psmDataDir, "latex", orchestrationUUID.toString()));
            Files.createDirectories(Path.of(this.psmDataDir, "reports", orchestrationUUID.toString()));
        } catch (Exception e) {
            logger.error("Could not create directories for orchestration data", e);
        }
    }

    private void createPsmDataDirIfNotExists() throws OrchestrationServiceException {
        try {
            Files.createDirectories(Path.of(this.psmDataDir, "raw"));
            Files.createDirectories(Path.of(this.psmDataDir, "plots"));
            Files.createDirectories(Path.of(this.psmDataDir, "latex"));
            Files.createDirectories(Path.of(this.psmDataDir, "reports"));
            this.logger.info("PSM data directory {} created", this.psmDataDir);
        } catch (IOException e) {
            logger.error("Could not create PMS data directory", e);
            throw new OrchestrationServiceException("Could not create PMS data directory", e);
        }
    }

    /* Report methods */
    @Async
    public void createReport(UUID orchestrationUUID, boolean cleanUp) throws OrchestrationServiceException {
        logger.info("Creating report for orchestration with uuid={}", orchestrationUUID);

        if (this.reportCreationLock.tryLock()) {
            try {
                // TODO: retrieve data from PSM

                // TODO: static
                this.collectStaticOrchestrationData(orchestrationUUID, cleanUp);

                // TODO: dynamic
                this.collectDynamicOrchestrationData(orchestrationUUID, cleanUp);

                // TODO: create report with psm

            } finally {
                this.reportCreationLock.unlock();
            }
        } else {
            logger.warn("Report creation is already in progress for orchestration with uuid={}", orchestrationUUID);
            throw new OrchestrationServiceException("Report creation is already in progress for orchestration with uuid=%s".formatted(orchestrationUUID));
        }

    }

    @PostConstruct
    public void initDefaultOrchestration() throws OrchestrationServiceException {
        Optional<Orchestration> orchestration = this.readOrchestrationWithPropertiesByName("default");

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

        // create data dir if not exists
        this.createPsmDataDirIfNotExists();
    }


}
