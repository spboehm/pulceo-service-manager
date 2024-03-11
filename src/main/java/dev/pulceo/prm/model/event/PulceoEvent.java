package dev.pulceo.prm.model.event;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@SuperBuilder
public class PulceoEvent implements Serializable {
    @Builder.Default
    String eventUUID = UUID.randomUUID().toString();
    @Builder.Default
    String timestamp = LocalDateTime.now().toString();
    EventType eventType;
    String payload;
}
