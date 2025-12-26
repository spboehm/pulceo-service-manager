package dev.pulceo.prm.dto.orchestration;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.Map;

@Data
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class CreateNewOrchestrationDTO {

    private String name;
    @Builder.Default
    private String description = "";
    @Builder.Default
    private Map<String, String> properties = new HashMap<>();

}
