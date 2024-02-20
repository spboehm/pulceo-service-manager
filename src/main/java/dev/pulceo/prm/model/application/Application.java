package dev.pulceo.prm.model.application;

import dev.pulceo.prm.dto.application.CreateNewApplicationDTO;
import dev.pulceo.prm.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
public class Application extends BaseEntity implements HasEndpoint {

    // uuid of application in super class
    private UUID remoteApplicationUUID; // the id on the local edge device
    private UUID nodeUUID; // the nodeUUID of the edge device (global id), not on the local edge device
    private String name;
    @OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE, CascadeType.REMOVE }, mappedBy = "application")
    private List<ApplicationComponent> applicationComponents;

    public static Application fromCreateNewApplicationDTO(CreateNewApplicationDTO createNewApplicationDTO) {
        return Application.builder()
                .name(createNewApplicationDTO.getName())
                .applicationComponents(createNewApplicationDTO.getApplicationComponents().stream().map(ApplicationComponent::fromCreateNewApplicationComponentDTO).toList())
                .build();
    }

    @Override
    public URI getEndpoint() {
        return URI.create("https://test.com");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Application that = (Application) o;

        if (!Objects.equals(name, that.name)) return false;
        return Objects.equals(applicationComponents, that.applicationComponents);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (applicationComponents != null ? applicationComponents.hashCode() : 0);
        return result;
    }

}
