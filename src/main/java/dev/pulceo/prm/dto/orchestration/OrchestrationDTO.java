package dev.pulceo.prm.dto.orchestration;

import dev.pulceo.prm.model.orchestration.Orchestration;
import dev.pulceo.prm.model.orchestration.OrchestrationStatus;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Data
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class OrchestrationDTO {

    private String name;
    private String description;
    private OrchestrationStatus status;
    private Map<String, String> properties;

    public static OrchestrationDTO fromOrchestration(Orchestration orchestration) {
        return OrchestrationDTO.builder()
                .name(orchestration.getName())
                .description(orchestration.getDescription())
                .status(orchestration.getStatus())
                .properties(orchestration.getProperties())
                .build();
    }
}
