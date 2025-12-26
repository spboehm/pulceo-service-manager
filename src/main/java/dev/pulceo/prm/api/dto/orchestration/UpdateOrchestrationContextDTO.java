package dev.pulceo.prm.api.dto.orchestration;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@Getter
@Setter
@NoArgsConstructor
public class UpdateOrchestrationContextDTO {

    private String uuid;
    private String name;

}
