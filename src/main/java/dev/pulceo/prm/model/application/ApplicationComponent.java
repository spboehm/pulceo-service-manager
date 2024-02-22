package dev.pulceo.prm.model.application;

import com.fasterxml.jackson.annotation.JsonBackReference;
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

    private UUID remoteApplicationComponentUUID; // on device
    private UUID nodeUUID;
    private String nodeHost;
    private String name;
    private String image;
    private int port;
    private String protocol;
    private ApplicationComponentType applicationComponentType;
    @JsonBackReference
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

    public static ApplicationComponent fromApplicationComponentDTO(UUID nodeUUID, String nodeHost, ApplicationComponentDTO applicationComponentDTO) {
        return ApplicationComponent.builder()
                .remoteApplicationComponentUUID(UUID.fromString(applicationComponentDTO.getApplicationComponentUUID()))
                .nodeUUID(nodeUUID)
                .nodeHost(nodeHost)
                .name(applicationComponentDTO.getName())
                .image(applicationComponentDTO.getImage())
                .port(applicationComponentDTO.getPort())
                .protocol(applicationComponentDTO.getProtocol())
                .applicationComponentType(applicationComponentDTO.getApplicationComponentType())
//                .environmentVariables(applicationComponentDTO.getEnvironmentVariables())
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
        if (this.applicationComponentType == ApplicationComponentType.PRIVATE) {
            return URI.create(this.getProtocol().toLowerCase() + "://" + this.application.getName() + "-" + this.getName() + ":" + this.getPort());
        } else {
            return URI.create(this.getProtocol().toLowerCase() + "://" + this.nodeHost + ":" + this.getPort());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ApplicationComponent that = (ApplicationComponent) o;

        if (port != that.port) return false;
        if (!Objects.equals(remoteApplicationComponentUUID, that.remoteApplicationComponentUUID))
            return false;
        if (!Objects.equals(nodeUUID, that.nodeUUID)) return false;
        if (!Objects.equals(nodeHost, that.nodeHost)) return false;
        if (!Objects.equals(name, that.name)) return false;
        if (!Objects.equals(image, that.image)) return false;
        if (!Objects.equals(protocol, that.protocol)) return false;
        if (applicationComponentType != that.applicationComponentType) return false;
        return Objects.equals(environmentVariables, that.environmentVariables);
    }

    @Override
    public int hashCode() {
        int result = remoteApplicationComponentUUID != null ? remoteApplicationComponentUUID.hashCode() : 0;
        result = 31 * result + (nodeUUID != null ? nodeUUID.hashCode() : 0);
        result = 31 * result + (nodeHost != null ? nodeHost.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (image != null ? image.hashCode() : 0);
        result = 31 * result + port;
        result = 31 * result + (protocol != null ? protocol.hashCode() : 0);
        result = 31 * result + (applicationComponentType != null ? applicationComponentType.hashCode() : 0);
        result = 31 * result + (environmentVariables != null ? environmentVariables.hashCode() : 0);
        return result;
    }
}
