package dev.pulceo.prm.dto.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

// usually sent from PNA
// TODO: maybe rename and move
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@SuperBuilder
public class ResourceMessage {

    private String sentBydeviceId;
    private ResourceType resourceType;
    private Operation operation;
    private String payload;
    
}
