package dev.pulceo.prm.service;

import dev.pulceo.prm.dto.node.NodeDTO;
import dev.pulceo.prm.exception.ApplicationServiceException;
import dev.pulceo.prm.model.application.Application;
import dev.pulceo.prm.model.application.ApplicationComponent;
import dev.pulceo.prm.repository.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;

    @Value("${prm.endpoint}")
    private String prmEndpoint;

    @Autowired
    public ApplicationService(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
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

//        WebClient webClientToPNA = WebClient.create(this.prmEndpoint);
//        webClientToPNA.post()
//                .uri("/api/v1/applications")
//                .bodyValue(application)
//                .retrieve()
//                .bodyToMono(Application.class)
//                .onErrorResume(error -> {
//                    throw new RuntimeException(new ApplicationServiceException("Can not create application: Node with id %s does not exist!".formatted(application.getNodeUUID())));
//                })
//                .block();

        Application persistedApplicationWithoutServices = this.applicationRepository.save(application);

        for (ApplicationComponent applicationComponent : application.getApplicationComponents()) {
            try {
                this.createApplicationComponent(persistedApplicationWithoutServices, applicationComponent);
            } catch (ApplicationServiceException e) {
                throw new ApplicationServiceException("Could not create application!", e);
            }
        }

        return persistedApplicationWithoutServices;
    }

    public ApplicationComponent createApplicationComponent(Application persistedApplicationWithputServices, ApplicationComponent applicationComponent) throws ApplicationServiceException {
        return ApplicationComponent.builder().build();
    }

    private boolean isApplicationAlreadyExisting(String name) {
        return this.applicationRepository.findByName(name).isPresent();
    }
}
