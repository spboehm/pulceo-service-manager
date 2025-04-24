package dev.pulceo.prm.dto.orchestration;

import dev.pulceo.prm.model.orchestration.OrchestrationContext;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Data
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class OrchestrationContextDTO {

    private String uuid;
    private String name;

    public static OrchestrationContextDTO fromOrchestrationContext(OrchestrationContext orchestrationContext) {
        return OrchestrationContextDTO.builder()
                .uuid(orchestrationContext.getOrchestration().getUuid().toString())
                .name(orchestrationContext.getOrchestration().getName())
                .build();
    }

}
