package dev.pulceo.prm.dto.pna;

import dev.pulceo.prm.dto.application.ApplicationComponentDTO;
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
@ToString
public class ApplicationOnPNADTO {

    private String applicationUUID; // local and remote id of the application
    private UUID nodeUUID; // the nodeUUID of the edge device (global id), not on the local edge device, remove because this is ambiguous
    private String endpoint; // the endpoint of the application
    private String name;
    @Builder.Default
    private List<ApplicationComponentDTO> applicationComponents = new ArrayList<>();



}
