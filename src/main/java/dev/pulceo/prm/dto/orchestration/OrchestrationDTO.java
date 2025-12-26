package dev.pulceo.prm.dto.orchestration;

import dev.pulceo.prm.model.orchestration.Orchestration;
import dev.pulceo.prm.model.orchestration.OrchestrationStatus;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Map;
import java.util.UUID;

@Data
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class OrchestrationDTO {

    @Builder.Default
    private String startTimestamp = "";
    @Builder.Default
    private String endTimestamp = "";
    private UUID uuid;
    private String name;
    private String description;
    private OrchestrationStatus status;
    private Map<String, String> properties;

    public static OrchestrationDTO fromOrchestration(Orchestration orchestration) {
        return OrchestrationDTO.builder()
                .startTimestamp(orchestration.getStartTimestamp() != null ? orchestration.getStartTimestamp().toString() : "")
                .endTimestamp(orchestration.getEndTimestamp() != null ? orchestration.getEndTimestamp().toString() : "")
                .uuid(orchestration.getUuid())
                .name(orchestration.getName())
                .description(orchestration.getDescription())
                .status(orchestration.getStatus())
                .properties(orchestration.getProperties())
                .build();
    }
}
