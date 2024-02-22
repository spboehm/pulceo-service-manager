package dev.pulceo.prm.dto.application;

import dev.pulceo.prm.model.application.ApplicationComponent;
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
public class ApplicationComponentDTO {

    private String applicationComponentUUID;
    private String name;
    private String endpoint;
    private String image;
    private int port;
    private String protocol;
    private ApplicationComponentType applicationComponentType;
//    @Builder.Default
//    private Map<String, String> environmentVariables = new HashMap<>();

    public static ApplicationComponentDTO fromApplicationComponent(ApplicationComponent applicationComponent) {
        return ApplicationComponentDTO.builder()
                .applicationComponentUUID(String.valueOf(applicationComponent.getUuid()))
                .name(applicationComponent.getName())
                .endpoint(String.valueOf(applicationComponent.getEndpoint()))
                .image(applicationComponent.getImage())
                .port(applicationComponent.getPort())
                .protocol(applicationComponent.getProtocol())
                .applicationComponentType(applicationComponent.getApplicationComponentType())
//                .environmentVariables(applicationComponent.getEnvironmentVariables())
                .build();
    }
}
