package dev.pulceo.prm.model.application;

import dev.pulceo.prm.dto.application.ApplicationDTO;
import dev.pulceo.prm.dto.application.CreateNewApplicationDTO;
import dev.pulceo.prm.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@NamedEntityGraph(
        name = "graph.Application.applicationComponents",
        attributeNodes = {
                @NamedAttributeNode("applicationComponents")
        }
)
@ToString(exclude = "applicationComponents")
public class Application extends BaseEntity implements HasEndpoint {

    // uuid of application in super class
    private UUID remoteApplicationUUID; // the id on the local edge device
    private UUID nodeUUID; // the nodeUUID of the edge device (global id), not on the local edge device
    private String name;
    @OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE, CascadeType.REMOVE }, mappedBy = "application")
    private List<ApplicationComponent> applicationComponents;

    public static Application fromCreateNewApplicationDTO(CreateNewApplicationDTO createNewApplicationDTO) {
        return Application.builder()
                .remoteApplicationUUID(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                .nodeUUID(createNewApplicationDTO.getNodeUUID())
                .name(createNewApplicationDTO.getName())
                .applicationComponents(createNewApplicationDTO.getApplicationComponents().stream().map(ApplicationComponent::fromCreateNewApplicationComponentDTO).toList())
                .build();
    }

    public static Application fromApplicationDTO(UUID nodeUUID, ApplicationDTO applicationDTO) {
        return Application.builder()
                .nodeUUID(nodeUUID)
                .remoteApplicationUUID(UUID.fromString(applicationDTO.getApplicationUUID()))
                .name(applicationDTO.getName())
                .applicationComponents(applicationDTO.getApplicationComponents().stream().map(ApplicationComponent::fromApplicationComponentDTO).toList())
                .build();
    }

    // TODO: fill with proper extension
    @Override
    public URI getEndpoint() {

        return URI.create("https://test.com");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Application that = (Application) o;

        if (!Objects.equals(remoteApplicationUUID, that.remoteApplicationUUID))
            return false;
        if (!Objects.equals(nodeUUID, that.nodeUUID)) return false;
        if (!Objects.equals(name, that.name)) return false;
        return Objects.equals(applicationComponents, that.applicationComponents);
    }

    @Override
    public int hashCode() {
        int result = remoteApplicationUUID != null ? remoteApplicationUUID.hashCode() : 0;
        result = 31 * result + (nodeUUID != null ? nodeUUID.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (applicationComponents != null ? applicationComponents.hashCode() : 0);
        return result;
    }
}
