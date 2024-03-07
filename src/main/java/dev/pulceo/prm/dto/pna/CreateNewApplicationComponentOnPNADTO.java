package dev.pulceo.prm.dto.pna;

import dev.pulceo.prm.model.application.ApplicationComponentType;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.Map;

@Data
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class CreateNewApplicationComponentOnPNADTO {

    private String name;
    private String image;
    private int port;
    private String protocol;
    private ApplicationComponentType applicationComponentType;
    @Builder.Default
    private Map<String, String> environmentVariables = new HashMap<>();


}
