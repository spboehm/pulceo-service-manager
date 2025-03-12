package dev.pulceo.prm.dto.task;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class CreateNewTaskDTO {
    @Builder.Default
    private Timestamp created = Timestamp.valueOf(LocalDateTime.now()); // timestamp where task is created on device
    @Builder.Default
    private String createdBy = ""; // user who created the task
    @Builder.Default
    private byte[] payload = new byte[0]; // payload of the task
    @Builder.Default
    private long sizeOfWorkload = 0; // size of the input data
    @Builder.Default
    private long sizeDuringTransmission = 0; // (compressed) size of workload during transmission
    @Builder.Default
    private int deadline = 100; // tolerable deadline in ms
    @Builder.Default
    private Map<String, String> requirements = new HashMap<>(); // requirements for the task
    @Builder.Default
    private Map<String, String> properties = new HashMap<>(); // properties of the task

}
