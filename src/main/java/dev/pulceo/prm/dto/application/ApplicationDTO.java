package dev.pulceo.prm.dto.application;

import dev.pulceo.prm.model.application.Application;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Data
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@ToString
public class ApplicationDTO {

    private String applicationUUID; // local and remote id of the application
    private String nodeUUID; // the nodeUUID of the edge device (global id), not on the local edge device
    private String name;
    @Builder.Default
    private List<ApplicationComponentDTO> applicationComponents = new ArrayList<>();

    public static ApplicationDTO fromApplication(Application application) {
        return ApplicationDTO.builder()
                .applicationUUID(String.valueOf(application.getUuid()))
                .nodeUUID(String.valueOf(application.getNodeUUID()))
                .name(application.getName())
                .applicationComponents(application.getApplicationComponents().stream().map(ApplicationComponentDTO::fromApplicationComponent).toList())
                .build();
    }

}
