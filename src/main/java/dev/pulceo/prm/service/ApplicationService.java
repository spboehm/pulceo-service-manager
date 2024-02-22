package dev.pulceo.prm.service;

import dev.pulceo.prm.dto.application.ApplicationDTO;
import dev.pulceo.prm.dto.node.NodeDTO;
import dev.pulceo.prm.exception.ApplicationServiceException;
import dev.pulceo.prm.model.application.Application;
import dev.pulceo.prm.model.application.ApplicationComponent;
import dev.pulceo.prm.repository.ApplicationComponentRepository;
import dev.pulceo.prm.repository.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationComponentRepository applicationComponentRepository;

    @Value("${prm.endpoint}")
    private String prmEndpoint;

    @Autowired
    public ApplicationService(ApplicationRepository applicationRepository, ApplicationComponentRepository applicationComponentRepository) {
        this.applicationRepository = applicationRepository;
        this.applicationComponentRepository = applicationComponentRepository;
    }

    public Application createApplication(Application application) throws ApplicationServiceException {
        if (this.isApplicationAlreadyExisting(application.getName())) {
            throw new ApplicationServiceException(String.format("Application %s already exists", application.getName()));
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

        WebClient webClientToPNA = WebClient.create("http://" + srcNode.getHostname() + ":" + "7676");
        ApplicationDTO applicationDTO = webClientToPNA.post()
                .uri("/api/v1/applications")
                .bodyValue(application)
                .retrieve()
                .bodyToMono(ApplicationDTO.class)
                .onErrorResume(error -> {
                    throw new RuntimeException(new ApplicationServiceException("Can not create application: Node with id %s does not exist!".formatted(application.getNodeUUID())));
                })
                .block();

        // if return is positive, persist application
        Application persistedApplication = this.applicationRepository.save(Application.fromApplicationDTO(srcNode.getUuid(), srcNode.getHostname(), applicationDTO));

        // only executed if element in list is present
//        for (ApplicationComponent applicationComponent : application.getApplicationComponents()) {
//            try {
//                this.createApplicationComponent(persistedApplication, applicationComponent);
//            } catch (ApplicationServiceException e) {
//                throw new ApplicationServiceException("Could not create application!", e);
//            }
//        }
        return persistedApplication;
    }

    public ApplicationComponent createApplicationComponent(Application application, ApplicationComponent applicationComponent) throws ApplicationServiceException {
        Optional<Application> persistedApplication = this.applicationRepository.findByName(application.getName());

        if (persistedApplication.isEmpty()) {
            throw new ApplicationServiceException(String.format("Application %s not found", application.getName()));
        }

        if (this.isApplicationComponentAlreadyExisting(applicationComponent.getName()) || isPortAlreadyInUse(applicationComponent.getPort())) {
            throw new ApplicationServiceException(String.format("ApplicationComponent %s already exists", applicationComponent.getName()));
        }

        return ApplicationComponent.builder().build();
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
}
