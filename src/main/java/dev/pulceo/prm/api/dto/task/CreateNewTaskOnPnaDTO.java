package dev.pulceo.prm.api.dto.task;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class CreateNewTaskOnPnaDTO {
    private UUID applicationUUID; // local application UUID on device (remote from psm)
    private UUID applicationComponentId; // local application component id on device (remote from psm)
    private byte[] payload = new byte[0]; // payload of the task
    @Builder.Default
    private String callbackProtocol = ""; // statically generated, never changed
    @Builder.Default
    private String callbackEndpoint = ""; // statically generated, never changed
    @Builder.Default
    private String destinationApplicationComponentProtocol = ""; // statically generated, never changed, e.g., http
    @Builder.Default
    private String destinationApplicationComponentEndpoint = ""; // statically generated, never changed, e.g., /api/test
    @Builder.Default
    private Map<String, String> properties = new HashMap<>(); // properties of the task
}
