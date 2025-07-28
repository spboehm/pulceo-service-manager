package dev.pulceo.prm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.pulceo.prm.api.*;
import dev.pulceo.prm.api.dto.metricexports.MetricType;
import dev.pulceo.prm.api.dto.orchestration.UpdateOrchestrationContextDTO;
import dev.pulceo.prm.api.dto.report.GenerateReportRequestDTO;
import dev.pulceo.prm.api.exception.PmsApiException;
import dev.pulceo.prm.api.exception.PrmApiException;
import dev.pulceo.prm.api.exception.PsmApiException;
import dev.pulceo.prm.dto.orchestration.OrchestrationContextDTO;
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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
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
    private final PrsApi prsApi;
    @Value("${psm.data.dir}")
    private String psmDataDir;
    private final Lock reportCreationLock = new ReentrantLock();

    @Autowired
    public OrchestrationService(OrchestrationRepository orchestrationRepository, OrchestrationContextRepository contextRepository, PsmApi psmApi, PrmApi prmApi, PmsApi pmsApi, PnaApi pnaApi, PrsApi prsApi) {
        this.orchestrationRepository = orchestrationRepository;
        this.contextRepository = contextRepository;
        this.psmApi = psmApi;
        this.prmApi = prmApi;
        this.pmsApi = pmsApi;
        this.pnaApi = pnaApi;
        this.prsApi = prsApi;
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

    public Optional<Orchestration> readOrchestrationByName(String name) {
        return this.orchestrationRepository.findByName(name);
    }

    public Optional<Orchestration> readOrchestrationWithPropertiesByUUID(UUID uuid) {
        return this.orchestrationRepository.findWithPropertiesByUuid(uuid);
    }

    public Optional<Orchestration> readOrchestrationByUUID(UUID uuid) {
        return this.orchestrationRepository.findByUuid(uuid);
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
                if (currentOrchestrationStatus == OrchestrationStatus.NEW && newOrchestrationStatus == OrchestrationStatus.RUNNING) {
                    // NEW -> RUNNING
                    updatedOrchestration.setStartTimestamp(Timestamp.valueOf(LocalDateTime.now()));
                } else if (currentOrchestrationStatus == OrchestrationStatus.RUNNING && newOrchestrationStatus == OrchestrationStatus.COMPLETED) {
                    // RUNNING -> COMPLETED
                    updatedOrchestration.setEndTimestamp(Timestamp.valueOf(LocalDateTime.now()));
                }
            } catch (OrchestrationServiceException e) {
                this.logger.error("Invalid status transition for Orchestration with uuid={}, name={} from {} to {}: {}",
                        optionalOrchestration.get().getUuid(), updatedOrchestration.getName(), currentOrchestrationStatus, newOrchestrationStatus, e.getMessage(), e);
                throw new OrchestrationServiceException("Invalid status transition for Orchestration with id=%s from %s to %s: %s".formatted(id, currentOrchestrationStatus, newOrchestrationStatus, e.getMessage()));
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
        Optional<Orchestration> optionalOrchestration = this.resolveOrchestrationWithProperties(id);
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

    public List<OrchestrationContextDTO> getAllOrchestrationContexts() throws OrchestrationServiceException {
        List<OrchestrationContextDTO> orchestrationContextDTOs = new ArrayList<>();
        // psm
        orchestrationContextDTOs.add(OrchestrationContextDTO.fromOrchestrationContext("psm", this.getOrCreateOrchestrationContext()));
        // prm API
        orchestrationContextDTOs.add(this.prmApi.getOrchestrationContext());
        // pms API
        orchestrationContextDTOs.add(this.pmsApi.getOrchestrationContext());
        return orchestrationContextDTOs;
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

            try {
                // update the orchestration context on PRM
                this.prmApi.updateOrchestrationContext(UpdateOrchestrationContextDTO.builder()
                        .uuid(context.getOrchestration().getUuid().toString())
                        .name(context.getOrchestration().getName())
                        .build());
            } catch (RuntimeException e) {
                this.logger.warn("Failed to update OrchestrationContext on PRM: {}", e.getMessage(), e);
            }

            // update the orchestration context on PMS
            try {
                this.pmsApi.updateOrchestrationContext(UpdateOrchestrationContextDTO.builder()
                        .uuid(context.getOrchestration().getUuid().toString())
                        .name(context.getOrchestration().getName())
                        .build());
            } catch (RuntimeException e) {
                this.logger.warn("Failed to update OrchestrationContext on PMS: {}", e.getMessage(), e);
            }
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
            return this.readOrchestrationByUUID(UUID.fromString(id));
        } else {
            return this.readOrchestrationWithPropertiesByName(id);
        }
    }


    private Optional<Orchestration> resolveOrchestrationWithProperties(String id) {
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

    public void reset() throws OrchestrationServiceException {
        this.pnaApi.resetAllPna();
        // reset PRM
        this.prmApi.resetOrchestrationContext();
        // reset PMS
        this.pmsApi.resetOrchestrationContext();
        // reset PSM
        this.resetOrchestrationContext();
    }

    private void resetOrchestrationContext() throws OrchestrationServiceException {
        OrchestrationContext context = this.getOrCreateOrchestrationContext();
        this.logger.info("Resetting OrchestrationContext with id={} to default orchestration", context.getId());
        Orchestration orchestration = context.getOrchestration();
        orchestration.setStartTimestamp(null);
        orchestration.setEndTimestamp(null);
        orchestration.setStatus(OrchestrationStatus.NEW);
        this.orchestrationRepository.save(orchestration);
        this.contextRepository.save(context);
        this.logger.info("OrchestrationContext with id={} successfully reset to orchestration with uuid={}, name={}, description={}, and status={}",
                context.getId(),
                orchestration.getUuid(),
                orchestration.getName(),
                orchestration.getDescription(),
                orchestration.getStatus());
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
                } catch (PmsApiException e) {
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

    public void generateAndSaveMetaFile(UUID orchestrationUUID) throws OrchestrationServiceException {
        Orchestration orchestration = this.readOrchestrationWithPropertiesByUUID(orchestrationUUID)
                .orElseThrow(() -> new OrchestrationServiceException("Orchestration with UUID %s not found".formatted(orchestrationUUID)));

        this.createDirsForOrchestrationData(orchestrationUUID);

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode jsonNode = objectMapper.createObjectNode();

        jsonNode.put("CREATED_AT", Timestamp.valueOf(LocalDateTime.now()).toInstant().toString());
        jsonNode.put("UUID", orchestration.getUuid().toString());
        jsonNode.put("NAME", orchestration.getName());
        jsonNode.put("START_TIMESTAMP", orchestration.getStartTimestamp() != null ? orchestration.getStartTimestamp().toInstant().toString() : "");
        jsonNode.put("END_TIMESTAMP", orchestration.getEndTimestamp() != null ? orchestration.getEndTimestamp().toInstant().toString() : "");
        jsonNode.put("DESCRIPTION", orchestration.getDescription());
        jsonNode.put("STATUS", orchestration.getStatus().toString());
        jsonNode.set("PROPERTIES", objectMapper.valueToTree(orchestration.getProperties()));

        try {
            Path metaFilePath = Path.of(this.psmDataDir, "raw", orchestrationUUID.toString(), "META.json");
            Files.writeString(metaFilePath, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode));
        } catch (JsonProcessingException e) {
            throw new OrchestrationServiceException("Failed to serialize meta file JSON", e);
        } catch (IOException e) {
            throw new OrchestrationServiceException("Failed to write meta file for orchestration with uuid=%s".formatted(orchestrationUUID), e);
        }
    }

    private void createDirsForOrchestrationData(UUID orchestrationUUID) throws OrchestrationServiceException {
        logger.info("Creating directories for orchestration data with uuid={}", orchestrationUUID);
        try {
            Files.createDirectories(Path.of(this.psmDataDir, "raw", orchestrationUUID.toString()));
            Files.createDirectories(Path.of(this.psmDataDir, "plots", orchestrationUUID.toString()));
            Files.createDirectories(Path.of(this.psmDataDir, "latex", orchestrationUUID.toString()));
            Files.createDirectories(Path.of(this.psmDataDir, "reports", orchestrationUUID.toString()));
        } catch (IOException e) {
            throw new OrchestrationServiceException("Could not create directories for orchestration data with uuid=%s".formatted(orchestrationUUID), e);
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

        // check if orchestration exists
        Optional<Orchestration> orchestrationOptional = this.readOrchestrationByUUID(orchestrationUUID);
        if (orchestrationOptional.isEmpty()) {
            logger.error("Orchestration with uuid={} not found", orchestrationUUID);
            throw new OrchestrationServiceException("Orchestration with uuid=%s not found".formatted(orchestrationUUID));
        }
        if (this.reportCreationLock.tryLock()) {
            try {
                this.collectStaticOrchestrationData(orchestrationUUID, cleanUp);
                this.collectDynamicOrchestrationData(orchestrationUUID, cleanUp);
                this.generateAndSaveMetaFile(orchestrationUUID);

                // generate report
                logger.info("Generating report for orchestration with uuid={}", orchestrationUUID);
                this.prsApi.checkHealth();
                GenerateReportRequestDTO generateOrchestrationReport = GenerateReportRequestDTO.builder()
                        .orchestrationUUID(orchestrationUUID)
                        .build();
                this.prsApi.generateOrchestrationReport(generateOrchestrationReport);
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
