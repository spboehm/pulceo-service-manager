package dev.pulceo.prm.dto.task;

import dev.pulceo.prm.model.task.Task;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class CreateNewTaskResponseDTO {

    private long taskSequenceNumber;
    private UUID taskUUID;
    private TaskMetaDataDTO taskMetaData;
    @Builder.Default
    private Timestamp created = Timestamp.valueOf(LocalDateTime.now()); // timestamp where task is created on device
    private String createdBy; // user who created the task
    @Builder.Default
    private Timestamp arrived = Timestamp.valueOf(LocalDateTime.now()); // timestamp where task arrived on device
    private String arrivedAt;
    private long sizeOfWorkload; // size of the input data
    private long sizeDuringTransmission; // (compressed) size of workload during transmissio
    private int deadline; // tolerable deadline in ms
    @Builder.Default
    private Map<String, String> requirements = new HashMap<>(); // requirements for the task
    @Builder.Default
    private Map<String, String> properties = new HashMap<>(); // properties of the task

    public static CreateNewTaskResponseDTO fromTask(Task task) {
        return CreateNewTaskResponseDTO.builder()
                .taskSequenceNumber(task.getTaskSequenceNumber())
                .taskUUID(task.getUuid())
                .taskMetaData(TaskMetaDataDTO.from(task.getTaskMetaData()))
                .created(task.getCreated())
                .createdBy(task.getCreatedBy())
                .arrived(task.getArrived())
                .arrivedAt(task.getArrivedAt())
                .sizeOfWorkload(task.getSizeOfWorkload())
                .sizeDuringTransmission(task.getSizeDuringTransmission())
                .deadline(task.getDeadline())
                .requirements(task.getRequirements())
                .properties(task.getProperties())
                .build();
    }
}
