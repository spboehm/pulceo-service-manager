package dev.pulceo.prm.dto.node;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@Getter
@Setter
@NoArgsConstructor
@ToString
@SuperBuilder
public class NodeDTO {

    private UUID uuid;
    private String providerName;
    private String hostname;
    private UUID pnaUUID;
    private NodePropertiesDTO node;

}
