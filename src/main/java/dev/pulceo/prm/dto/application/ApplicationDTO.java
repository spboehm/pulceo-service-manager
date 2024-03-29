package dev.pulceo.prm.dto.application;

import dev.pulceo.prm.model.application.Application;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ApplicationDTO {

    private String applicationUUID; // local and remote id of the application
    private UUID remoteApplicationUUID;
    private String nodeId; // the nodeUUID of the edge device (global id), not on the local edge device, remove because this is ambiguous
    private String endpoint; // the endpoint of the application
    private String name;
//    @Builder.Default
//    private List<ApplicationComponentDTO> applicationComponents = new ArrayList<>();

    public static ApplicationDTO fromApplication(Application application) {
        return ApplicationDTO.builder()
                .applicationUUID(String.valueOf(application.getUuid()))
                .remoteApplicationUUID(application.getRemoteApplicationUUID())
                .nodeId(String.valueOf(application.getNodeId()))
                .endpoint(String.valueOf(application.getEndpoint()))
                .name(application.getName())
//                .applicationComponents(application.getApplicationComponents().stream().map(ApplicationComponentDTO::fromApplicationComponent).toList())
                .build();
    }

}
