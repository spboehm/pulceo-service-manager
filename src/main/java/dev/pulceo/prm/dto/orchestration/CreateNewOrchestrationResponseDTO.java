package dev.pulceo.prm.dto.orchestration;

import dev.pulceo.prm.model.orchestration.Orchestration;
import dev.pulceo.prm.model.orchestration.OrchestrationStatus;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class CreateNewOrchestrationResponseDTO {

    private UUID uuid;
    private String name;
    private String description = "";
    private OrchestrationStatus status = OrchestrationStatus.NEW;
    private Map<String, String> properties = new HashMap<>();

    public static CreateNewOrchestrationResponseDTO fromOrchestration(Orchestration orchestration) {
        return CreateNewOrchestrationResponseDTO.builder()
                .uuid(orchestration.getUuid())
                .name(orchestration.getName())
                .description(orchestration.getDescription())
                .status(orchestration.getStatus())
                .properties(orchestration.getProperties())
                .build();
    }
}
