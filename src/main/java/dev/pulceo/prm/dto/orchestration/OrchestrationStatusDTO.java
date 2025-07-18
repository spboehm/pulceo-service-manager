package dev.pulceo.prm.dto.orchestration;

import dev.pulceo.prm.model.orchestration.OrchestrationStatus;
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
public class OrchestrationStatusDTO {
    
    private OrchestrationStatus orchestrationStatus;

}
