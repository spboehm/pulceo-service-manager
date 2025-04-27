package dev.pulceo.prm.service;

import dev.pulceo.prm.dto.node.NodeDTO;
import dev.pulceo.prm.dto.pna.ApplicationOnPNADTO;
import dev.pulceo.prm.exception.ApplicationServiceException;
import dev.pulceo.prm.model.application.Application;
import dev.pulceo.prm.model.application.ApplicationComponent;
import dev.pulceo.prm.model.application.ApplicationComponentType;
import dev.pulceo.prm.model.event.EventType;
import dev.pulceo.prm.model.event.PulceoEvent;
import dev.pulceo.prm.repository.ApplicationComponentRepository;
import dev.pulceo.prm.repository.ApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class ApplicationService {

    Logger logger = LoggerFactory.getLogger(ApplicationService.class);

    private final ApplicationRepository applicationRepository;
    private final ApplicationComponentRepository applicationComponentRepository;

    @Value("${prm.endpoint}")
    private String prmEndpoint;

    @Value("${webclient.scheme}")
    private String webClientScheme;

    private final EventHandler eventHandler;

    @Autowired
    public ApplicationService(ApplicationRepository applicationRepository, ApplicationComponentRepository applicationComponentRepository, EventHandler eventHandler) {
        this.applicationRepository = applicationRepository;
        this.applicationComponentRepository = applicationComponentRepository;
        this.eventHandler = eventHandler;
    }

    public Application createPreliminaryApplication(Application application) throws ApplicationServiceException {
        // TODO: check if target node exists
        WebClient webClientToPRM = WebClient.create(this.prmEndpoint);
        NodeDTO srcNode = webClientToPRM.get()
                .uri("/api/v1/nodes/" + application.getNodeId())
                .retrieve()
                .bodyToMono(NodeDTO.class)
                .onErrorResume(error -> {
                    throw new RuntimeException(new ApplicationServiceException("Can not create application: Node with id %s does not exist!".formatted(application.getNodeId())));
                })
                .block();
        application.setName(srcNode.getNode().getName() + "-" + application.getName());
        application.setNodeId(String.valueOf(srcNode.getUuid()));
        Optional<Application> newApplication = this.applicationRepository.findByName(srcNode.getNode().getName() + "-" + application.getName());
        if (newApplication.isPresent()) {
            logger.error("Application with name {} already exists!", application.getName());
            throw new ApplicationServiceException("Application with name " + application.getName() + " already exists!");
        }

        for (ApplicationComponent applicationComponent : application.getApplicationComponents()) {
            if (applicationComponent.getApplicationComponentType() == ApplicationComponentType.PUBLIC) {
                if (!isApplicationComponentDeployable(srcNode.getUuid(), applicationComponent.getPort(), ApplicationComponentType.PUBLIC, applicationComponent.isDeployed())) {
                    logger.error("Application component with name {} already exists!", applicationComponent.getName());
                    throw new ApplicationServiceException("Port " + applicationComponent.getPort() + " is already in use!");
                }
            }
        }
        return this.applicationRepository.save(application);
    }


    @Async
    public CompletableFuture<Application> createApplicationAsync(Application application) throws ApplicationServiceException, InterruptedException {
        logger.info("Creating application asynchronously: " + application.toString());
        Optional<Application> preliminaryApplication = this.applicationRepository.findByUuid(application.getUuid());
        if (preliminaryApplication.isEmpty()) {
            logger.error("Application {} does not exist. Please create at first lazily", application.getUuid());
            throw new ApplicationServiceException("Application %s does not exist. Please create at first lazily");
        }

        // TODO: Webclient to pna, try to commit
        WebClient webClientToPRM = WebClient.create(this.prmEndpoint);
        NodeDTO srcNode = webClientToPRM.get()
                .uri("/api/v1/nodes/" + application.getNodeId())
                .retrieve()
                .bodyToMono(NodeDTO.class)
                .onErrorResume(error -> {
                    this.applicationRepository.delete(preliminaryApplication.get());
                    throw new RuntimeException(new ApplicationServiceException("Can not create application: Node with id %s does not exist!".formatted(application.getNodeId())));
                })
                .block();

        WebClient webClientToPNA = WebClient.create(this.webClientScheme + "://" + srcNode.getHostname() + ":" + "7676");
        ApplicationOnPNADTO applicationDTO = webClientToPNA.post()
                .uri("/api/v1/applications")
                .header("Authorization", "Basic " + getPnaTokenByNodeUUID(srcNode.getUuid()))
                .bodyValue(application)
                .retrieve()
                .bodyToMono(ApplicationOnPNADTO.class)
                .onErrorResume(error -> {
                    throw new RuntimeException(new ApplicationServiceException("Can not create application: Node with id %s does not exist!".formatted(application.getNodeId())));
                })
                .block();

        // if return is positive, persist application
        logger.debug("Received application response from PNA: " + applicationDTO.toString());
        Application receivedApplication = Application.fromApplicationDTO(srcNode.getUuid(), srcNode.getHostname(), application.getNodeId(), applicationDTO);

        preliminaryApplication.get().setRemoteApplicationUUID(receivedApplication.getRemoteApplicationUUID());
        preliminaryApplication.get().setNodeId(receivedApplication.getNodeId());
        preliminaryApplication.get().setName(receivedApplication.getName());

        Application persistedApplication = this.applicationRepository.save(preliminaryApplication.get());
        // only executed if element in list is present
        for (ApplicationComponent applicationComponent : receivedApplication.getApplicationComponents()) {
            try {
                this.createApplicationComponent(persistedApplication, applicationComponent);
            } catch (ApplicationServiceException e) {
                logger.error("Could not create application component!", e);
                throw new ApplicationServiceException("Could not create application!", e);
            }
        }

        Optional<Application> fullyPersistedApplication = this.readApplicationByUUID(persistedApplication.getUuid());

        if (fullyPersistedApplication.isEmpty()) {
            throw new ApplicationServiceException("Could not retrieve application!");
        }

        PulceoEvent pulceoEvent = PulceoEvent.builder()
                .eventType(EventType.APPLICATION_CREATED)
                .payload(fullyPersistedApplication.get().toString())
                .build();
        this.eventHandler.handleEvent(pulceoEvent);

        return CompletableFuture.completedFuture(persistedApplication);
    }

    private String getPnaTokenByNodeUUID(UUID nodeUUID) {
        WebClient webClient = WebClient.create(this.prmEndpoint);
        String pnaToken = webClient.get()
                .uri("/api/v1/nodes/" + nodeUUID + "/pna-token")
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(error -> {
                    throw new RuntimeException(new ApplicationServiceException("Can not create link: Source node with id %s does not exist!".formatted(nodeUUID)));
                })
                .block();
        return pnaToken;
    }

    public ApplicationComponent createApplicationComponent(Application application, ApplicationComponent applicationComponent) throws ApplicationServiceException, InterruptedException {
        logger.info("Creating application component for " + application.toString(), applicationComponent.toString());
        Optional<Application> persistedApplication = this.applicationRepository.findByName(application.getName());
        applicationComponent.setName(application.getName() + "-" + applicationComponent.getName());

        if (persistedApplication.isEmpty()) {
            logger.error("Application {} not found", application.getName());
            throw new ApplicationServiceException(String.format("Application %s not found", application.getName()));
        }

        if (!this.isApplicationComponentDeployable(UUID.fromString(application.getNodeId()), applicationComponent.getPort(), applicationComponent.getApplicationComponentType(), applicationComponent.isDeployed())) {
            logger.error("ApplicationComponent {} already exists", applicationComponent.getName());
            throw new ApplicationServiceException(String.format("ApplicationComponent %s already exists", applicationComponent.getName()));
        }
        applicationComponent.setApplication(persistedApplication.get());

        PulceoEvent pulceoEvent = PulceoEvent.builder()
                .eventType(EventType.APPLICATION_COMPONENT_CREATED)
                .payload(applicationComponent.toString())
                .build();
        this.eventHandler.handleEvent(pulceoEvent);
        applicationComponent.setDeployed(true);
        return this.applicationComponentRepository.save(applicationComponent);
    }

    public List<Application> readAllApplications() {
        List<Application> applications = new ArrayList<>();
        Iterable<Application> applicationIterable = this.applicationRepository.findAll();
        applicationIterable.forEach(applications::add);
        return applications;
    }

    private boolean isApplicationAlreadyExisting(String name) {
        return this.applicationRepository.findByName(name).isPresent();
    }

    private boolean isApplicationComponentDeployable(UUID nodeUUID, int port, ApplicationComponentType applicationComponentType, boolean isDeployed) {
        Optional<ApplicationComponent> applicationComponent = this.applicationComponentRepository.findByNodeUUIDAndPortAndApplicationComponentType(nodeUUID, port, applicationComponentType);
        if (applicationComponent.isPresent()) {
            if (!applicationComponent.get().isDeployed()) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    private boolean isApplicationComponentAlreadyExisting(String name) {
        return this.applicationComponentRepository.findByName(name).isPresent();
    }

    private boolean isPortAlreadyInUse(int port) {
        return this.applicationComponentRepository.findByPort(port).isPresent();
    }

    public Optional<Application> readApplicationByUUID(UUID uuid) {
        return this.applicationRepository.findByUuid(uuid);
    }

    @Async
    public void deleteApplication(UUID uuid) throws ApplicationServiceException, InterruptedException {
        Optional<Application> optionalApplication = this.applicationRepository.findByUuid(uuid);

        if (optionalApplication.isEmpty()) {
            throw new ApplicationServiceException("Application with UUID " + uuid + " does not exist!");
        }

        Application application = optionalApplication.get();

        // TODO: Webclient to pna, try to commit
        WebClient webClientToPRM = WebClient.create(this.prmEndpoint);
        NodeDTO srcNode = webClientToPRM.get()
                .uri("/api/v1/nodes/" + application.getNodeId())
                .retrieve()
                .bodyToMono(NodeDTO.class)
                .onErrorResume(error -> {
                    throw new RuntimeException(new ApplicationServiceException("Can not create application: Node with id %s does not exist!".formatted(application.getNodeId())));
                })
                .block();

        WebClient webClientToPNA = WebClient.create(this.webClientScheme + "://" + srcNode.getHostname() + ":" + "7676");
        webClientToPNA.delete()
                .uri("/api/v1/applications/" + application.getRemoteApplicationUUID())
                .header("Authorization", "Basic " + getPnaTokenByNodeUUID(srcNode.getUuid()))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                    throw new RuntimeException(new ApplicationServiceException("Could not delete application"));
                })
                .bodyToMono(Void.class)
                .block();

        PulceoEvent pulceoEvent = PulceoEvent.builder()
                .eventType(EventType.APPLICATION_DELETED)
                .payload(application.toString())
                .build();
        this.eventHandler.handleEvent(pulceoEvent);

        this.applicationRepository.delete(application);
    }


    public Optional<Application> readApplicationByName(String id) {
        return this.applicationRepository.findByName(id);
    }

    public void reset() {
        this.applicationComponentRepository.deleteAll();
        this.applicationRepository.deleteAll();
    }
}
