package dev.pulceo.prm.service;

import dev.pulceo.prm.dto.application.ApplicationDTO;
import dev.pulceo.prm.dto.node.NodeDTO;
import dev.pulceo.prm.exception.ApplicationServiceException;
import dev.pulceo.prm.model.application.Application;
import dev.pulceo.prm.model.application.ApplicationComponent;
import dev.pulceo.prm.model.application.ApplicationComponentType;
import dev.pulceo.prm.repository.ApplicationComponentRepository;
import dev.pulceo.prm.repository.ApplicationRepository;
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

    private final ApplicationRepository applicationRepository;
    private final ApplicationComponentRepository applicationComponentRepository;

    @Value("${prm.endpoint}")
    private String prmEndpoint;

    @Value("${webclient.scheme}")
    private String webClientScheme;

    @Autowired
    public ApplicationService(ApplicationRepository applicationRepository, ApplicationComponentRepository applicationComponentRepository) {
        this.applicationRepository = applicationRepository;
        this.applicationComponentRepository = applicationComponentRepository;
    }

    public Application createPreliminaryApplication(Application application) throws ApplicationServiceException {
        Optional<Application> newApplication = this.applicationRepository.findByName(application.getName());
        if (newApplication.isPresent()) {
            throw new ApplicationServiceException("Application with name " + application.getName() + " already exists!");
        }

        for (ApplicationComponent applicationComponent : application.getApplicationComponents()) {
            if (applicationComponent.getApplicationComponentType() == ApplicationComponentType.PUBLIC) {
                Optional<ApplicationComponent> applicationComponentOptional = this.applicationComponentRepository.findByPortAndApplicationComponentType(applicationComponent.getPort(), ApplicationComponentType.PUBLIC);
                if (applicationComponentOptional.isPresent()) {
                    throw new ApplicationServiceException("Port " + applicationComponent.getPort() + " is already in use!");
                }
            }
        }
        return this.applicationRepository.save(application);
    }


    @Async
    public CompletableFuture<Application> createApplicationAsync(Application application) throws ApplicationServiceException {
        Optional<Application> preliminaryApplication = this.applicationRepository.findByUuid(application.getUuid());
        if (preliminaryApplication.isEmpty()) {
            throw new ApplicationServiceException("Application %s does not exist. Please create at first lazily");
        }

        // TODO: Webclient to pna, try to commit
        WebClient webClientToPRM = WebClient.create(this.prmEndpoint);
        NodeDTO srcNode = webClientToPRM.get()
                .uri("/api/v1/nodes/" + application.getNodeUUID())
                .retrieve()
                .bodyToMono(NodeDTO.class)
                .onErrorResume(error -> {
                    throw new RuntimeException(new ApplicationServiceException("Can not create application: Node with id %s does not exist!".formatted(application.getNodeUUID())));
                })
                .block();

        WebClient webClientToPNA = WebClient.create(this.webClientScheme + "://" + srcNode.getHostname() + ":" + "7676");
        ApplicationDTO applicationDTO = webClientToPNA.post()
                .uri("/api/v1/applications")
                .header("Authorization", "Basic " + getPnaTokenByNodeUUID(srcNode.getUuid()))
                .bodyValue(application)
                .retrieve()
                .bodyToMono(ApplicationDTO.class)
                .onErrorResume(error -> {
                    throw new RuntimeException(new ApplicationServiceException("Can not create application: Node with id %s does not exist!".formatted(application.getNodeUUID())));
                })
                .block();

        // if return is positive, persist application
        Application receivedApplication = Application.fromApplicationDTO(srcNode.getUuid(), srcNode.getHostname(), applicationDTO);

        preliminaryApplication.get().setRemoteApplicationUUID(receivedApplication.getRemoteApplicationUUID());
        preliminaryApplication.get().setNodeUUID(receivedApplication.getNodeUUID());
        preliminaryApplication.get().setName(receivedApplication.getName());

        Application persistedApplication = this.applicationRepository.save(preliminaryApplication.get());
        // only executed if element in list is present
        for (ApplicationComponent applicationComponent : receivedApplication.getApplicationComponents()) {
            try {
                this.createApplicationComponent(persistedApplication, applicationComponent);
            } catch (ApplicationServiceException e) {
                throw new ApplicationServiceException("Could not create application!", e);
            }
        }
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

    public ApplicationComponent createApplicationComponent(Application application, ApplicationComponent applicationComponent) throws ApplicationServiceException {
        Optional<Application> persistedApplication = this.applicationRepository.findByName(application.getName());

        if (persistedApplication.isEmpty()) {
            throw new ApplicationServiceException(String.format("Application %s not found", application.getName()));
        }

        if (this.isApplicationComponentAlreadyExisting(applicationComponent.getName()) || isPortAlreadyInUse(applicationComponent.getPort())) {
            throw new ApplicationServiceException(String.format("ApplicationComponent %s already exists", applicationComponent.getName()));
        }
        applicationComponent.setApplication(persistedApplication.get());
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
    public void deleteApplication(UUID uuid) throws ApplicationServiceException {
        Optional<Application> optionalApplication = this.applicationRepository.findByUuid(uuid);

        if (optionalApplication.isEmpty()) {
            throw new ApplicationServiceException("Application with UUID " + uuid + " does not exist!");
        }

        Application application = optionalApplication.get();

        // TODO: Webclient to pna, try to commit
        WebClient webClientToPRM = WebClient.create(this.prmEndpoint);
        NodeDTO srcNode = webClientToPRM.get()
                .uri("/api/v1/nodes/" + application.getNodeUUID())
                .retrieve()
                .bodyToMono(NodeDTO.class)
                .onErrorResume(error -> {
                    throw new RuntimeException(new ApplicationServiceException("Can not create application: Node with id %s does not exist!".formatted(application.getNodeUUID())));
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

        this.applicationRepository.delete(application);
    }


    public Optional<Application> readApplicationByName(String id) {
        return this.applicationRepository.findByName(id);
    }
}
