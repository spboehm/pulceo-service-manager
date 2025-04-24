package dev.pulceo.prm.model.orchestration;

import dev.pulceo.prm.dto.orchestration.CreateNewOrchestrationDTO;
import dev.pulceo.prm.model.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.proxy.HibernateProxy;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@NamedEntityGraph(
        name = "graph.orchestration.properties",
        attributeNodes = {
                @NamedAttributeNode("properties")
        }
)
public class Orchestration extends BaseEntity {

    @NotBlank(message = "Orchestration name cannot be blank!")
    private String name;
    @Builder.Default
    private String description = "";
    @Builder.Default
    private OrchestrationStatus status = OrchestrationStatus.NEW;
    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @MapKeyColumn(name = "orchestration_property_key")
    @Column(name = "orchestration_property_value")
    @CollectionTable(name = "orchestration_properties", joinColumns = @JoinColumn(name = "orchestration_property_id"))
    private Map<String, String> properties = new HashMap<>();

    public static Orchestration fromCreateNewOrchestrationDTO(CreateNewOrchestrationDTO createNewOrchestrationDTO) {
        return Orchestration.builder()
                .name(createNewOrchestrationDTO.getName())
                .description(createNewOrchestrationDTO.getDescription())
                .properties(createNewOrchestrationDTO.getProperties())
                .build();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Orchestration that = (Orchestration) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

}
