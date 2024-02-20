package dev.pulceo.prm.model.application;

import dev.pulceo.prm.dto.application.ApplicationComponentDTO;
import dev.pulceo.prm.dto.application.CreateNewApplicationComponentDTO;
import dev.pulceo.prm.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.net.URI;
import java.util.*;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationComponent extends BaseEntity implements HasEndpoint {

    private UUID nodeUUID;
    private String nodeHost;
    private String name;
    private String image;
    private int port;
    private String protocol;
    private ApplicationComponentType applicationComponentType;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private Application application;
    @ElementCollection
    @MapKeyColumn(name="envKey")
    @Column(name="envValue")
    @CollectionTable(name = "application_component_environment_variables", joinColumns = @JoinColumn(name = "application_component_id"))
    @Builder.Default
    private Map<String, String> environmentVariables = new HashMap<>();

    public void addApplication(Application application) {
        this.application = application;
    }

    public static ApplicationComponent fromApplicationComponentDTO(ApplicationComponentDTO applicationComponentDTO) {
        return ApplicationComponent.builder()
                .name(applicationComponentDTO.getName())
                .image(applicationComponentDTO.getImage())
                .port(applicationComponentDTO.getPort())
                .protocol(applicationComponentDTO.getProtocol())
                .applicationComponentType(applicationComponentDTO.getApplicationComponentType())
                //.environmentVariables(applicationComponentDTO.getEnvironmentVariables())
                .build();
    }

    public static ApplicationComponent fromCreateNewApplicationComponentDTO(CreateNewApplicationComponentDTO createNewApplicationComponentDTO) {
        return ApplicationComponent.builder()
                .name(createNewApplicationComponentDTO.getName())
                .image(createNewApplicationComponentDTO.getImage())
                .port(createNewApplicationComponentDTO.getPort())
                .protocol(createNewApplicationComponentDTO.getProtocol())
                .applicationComponentType(createNewApplicationComponentDTO.getApplicationComponentType())
                .environmentVariables(createNewApplicationComponentDTO.getEnvironmentVariables())
                .build();

    }

    private String getKubernetesServiceProtocol() {
        if ("UDP".equalsIgnoreCase(this.protocol)) {
            return "UDP";
        } else {
           return "TCP";
        }
    }

    @Override
    public URI getEndpoint() {
        return URI.create(this.getProtocol().toLowerCase() + "://" + this.nodeHost + ":" + this.getPort());
    }

}
