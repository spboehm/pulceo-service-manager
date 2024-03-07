package dev.pulceo.prm.model.application;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import dev.pulceo.prm.dto.application.ApplicationDTO;
import dev.pulceo.prm.dto.application.CreateNewApplicationDTO;
import dev.pulceo.prm.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.proxy.HibernateProxy;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@NamedEntityGraph(
        name = "graph.Application.applicationComponents",
        attributeNodes = {
                @NamedAttributeNode("applicationComponents")
        }
)
public class Application extends BaseEntity implements HasEndpoint {

    // uuid of application in super class
    private UUID remoteApplicationUUID; // the id on the local edge device
    private UUID nodeUUID; // the nodeUUID of the edge device (global id), not on the local edge device
    private String name;
    @JsonManagedReference
    @Builder.Default
    @OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE, CascadeType.REMOVE }, mappedBy = "application")
    private List<ApplicationComponent> applicationComponents = new ArrayList<>();

    public static Application fromCreateNewApplicationDTO(CreateNewApplicationDTO createNewApplicationDTO) {
        return Application.builder()
                .remoteApplicationUUID(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                .nodeUUID(createNewApplicationDTO.getNodeUUID())
                .name(createNewApplicationDTO.getName())
                .applicationComponents(createNewApplicationDTO.getApplicationComponents().stream().map(ApplicationComponent::fromCreateNewApplicationComponentDTO).toList())
                .build();
    }

    public static Application fromApplicationDTO(UUID nodeUUID, String nodeHost, ApplicationDTO applicationDTO) {
        return Application.builder()
                .nodeUUID(nodeUUID)
                .remoteApplicationUUID(UUID.fromString(applicationDTO.getApplicationUUID()))
                .name(applicationDTO.getName())
                .applicationComponents(applicationDTO.getApplicationComponents().stream().map(applicationComponentDTO -> ApplicationComponent.fromApplicationComponentDTO(nodeUUID, nodeHost, applicationComponentDTO)).toList())
                .build();
    }

    // TODO: fill with proper extension
    @Override
    public URI getEndpoint() {
        for (ApplicationComponent applicationComponent : applicationComponents) {
            if (applicationComponent.getApplicationComponentType().equals(ApplicationComponentType.PUBLIC)) {
                return applicationComponent.getEndpoint();
            }
        }
        return URI.create("");
    }

    public void addApplicationComponent(ApplicationComponent applicationComponent) {
        this.applicationComponents.add(applicationComponent);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Application that = (Application) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
