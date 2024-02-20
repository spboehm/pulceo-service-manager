package dev.pulceo.prm.dto.node;

import lombok.*;

import java.util.UUID;

@Data
@Getter
@Setter
@NoArgsConstructor
@ToString
public class NodeDTO  {

    private UUID uuid;
    private String providerName;
    private String hostname;
    private UUID pnaUUID;
    private NodePropertiesDTO node;

}
