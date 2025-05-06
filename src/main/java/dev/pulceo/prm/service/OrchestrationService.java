package dev.pulceo.prm.service;

import dev.pulceo.prm.api.PmsApi;
import dev.pulceo.prm.api.PnaApi;
import dev.pulceo.prm.api.PrmApi;
import dev.pulceo.prm.api.PsmApi;
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
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

    public void collectAllOrchestrationData() throws OrchestrationServiceException {
        OrchestrationContext orchestrationContext = this.getOrCreateOrchestrationContext();
        // create dirs for orchestration data
        this.createDirsForOrchestrationData(orchestrationContext.getOrchestration().getUuid());

        // TODO: static data
        this.collectStaticOrchestrationData(orchestrationContext.getOrchestration().getUuid());
    }

    public void collectDynamicOrchestrationData(UUID orchestrationUuid) throws OrchestrationServiceException {
        this.createDirsForOrchestrationData(orchestrationUuid);

        // TODO: Metrics

        // TODO: CPU Utilization
        byte[] cpuUtilizationRaw = this.pmsApi.getAllCpuUtilizationRaw();
        this.saveAsJson(cpuUtilizationRaw, "raw", orchestrationUuid.toString(), "CPU_UTIL.csv");

        // TODO: Memory Utilization

        // TODO: Storage Utilization

        // TODO: Network

        // TODO: ICMP RTT

        // TODO: TCP BW

        // TODO: UDP BW

        // TODO: REQUESTS

        // TODO: EVENTS

        // TODO: Task Status Logs

    }

    public void collectStaticOrchestrationData(UUID orchestrationUuid) throws OrchestrationServiceException {
        this.createDirsForOrchestrationData(orchestrationUuid);
        // TODO: PROVIDERS
        byte[] providersRaw = this.prmApi.getAllProvidersRaw();
        this.saveAsJson(providersRaw, "raw", orchestrationUuid.toString(), "PROVIDERS.json");

        // NODES
        byte[] nodesRaw = this.prmApi.getAllNodesRaw();
        this.saveAsJson(nodesRaw, "raw", orchestrationUuid.toString(), "NODES.json");

        // LINKS
        byte[] linksRaw = this.prmApi.getAllLinksRaw();
        this.saveAsJson(linksRaw, "raw", orchestrationUuid.toString(), "LINKS.json");

        // CPUS
        byte[] cpusRaw = this.prmApi.getAllCpusRaw();
        this.saveAsJson(cpusRaw, "raw", orchestrationUuid.toString(), "CPUS.json");

        // MEMORY
        byte[] memoryRaw = this.prmApi.getAllMemoryRaw();
        this.saveAsJson(memoryRaw, "raw", orchestrationUuid.toString(), "MEMORY.json");

        // STORAGE
        byte[] storageRaw = this.prmApi.getAllStorageRaw();
        this.saveAsJson(storageRaw, "raw", orchestrationUuid.toString(), "STORAGE.json");

        // Applications
        byte[] applicationsRaw = this.psmApi.getAllApplicationsRaw();
        this.saveAsJson(applicationsRaw, "raw", orchestrationUuid.toString(), "APPLICATIONS.json");

        // Metrics Requests
        byte[] metricsRequestsRaw = this.pmsApi.getAllMetricRequestsRaw();
        this.saveAsJson(metricsRequestsRaw, "raw", orchestrationUuid.toString(), "METRICS_REQUESTS.json");
    }

    public void saveAsJson(byte[] raw, String subfolder, String orchestrationUuid, String fileName) throws OrchestrationServiceException {
        try {
            Path filePath = Path.of(this.psmDataDir, subfolder, orchestrationUuid, fileName);
            Files.write(filePath, raw); // Write the raw data to the file
            this.logger.info("Saved raw data to {}", filePath);
        } catch (IOException e) {
            this.logger.error("Failed to save raw data to {}/{}", subfolder, fileName, e);
            throw new OrchestrationServiceException("Failed to save raw data", e);
        }
    }

    private void createDirsForOrchestrationData(UUID orchestrationUUID) {
        logger.info("Creating directories for orchestration data with uuid={}", orchestrationUUID);
        try {
            Files.createDirectories(Path.of(this.psmDataDir, "raw", orchestrationUUID.toString()));
            Files.createDirectories(Path.of(this.psmDataDir, "plots", orchestrationUUID.toString()));
            Files.createDirectories(Path.of(this.psmDataDir, "latex", orchestrationUUID.toString()));
            Files.createDirectories(Path.of(this.psmDataDir, "reports", orchestrationUUID.toString()));
        } catch (IOException e) {
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
