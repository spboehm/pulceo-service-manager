package dev.pulceo.prm.model.application;

import com.fasterxml.jackson.annotation.JsonBackReference;
import dev.pulceo.prm.dto.application.ApplicationComponentDTO;
import dev.pulceo.prm.dto.application.CreateNewApplicationComponentDTO;
import dev.pulceo.prm.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.proxy.HibernateProxy;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

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
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        ApplicationComponent that = (ApplicationComponent) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
