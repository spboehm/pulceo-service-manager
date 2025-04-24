package dev.pulceo.prm.model.orchestration;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class OrchestrationContext {

    @Id
    private Long id = 1L;
    @OneToOne(optional = false)
    @JoinColumn(name = "orchestration_id", referencedColumnName = "id")
    private Orchestration orchestration;

}
