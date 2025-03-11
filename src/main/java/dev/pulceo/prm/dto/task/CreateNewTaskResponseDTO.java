package dev.pulceo.prm.dto.task;

import dev.pulceo.prm.model.task.Task;
import dev.pulceo.prm.model.task.TaskMetaData;
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

    private UUID taskUUID;
    private TaskMetaDataDTO taskMetaData;
    @Builder.Default
    private Timestamp created = Timestamp.valueOf(LocalDateTime.now()); // timestamp where task is created on device
    @Builder.Default
    private Timestamp arrived = Timestamp.valueOf(LocalDateTime.now()); // timestamp where task arrived on device
    private long sizeOfWorkload; // size of the input data
    private long sizeDuringTransmission; // (compressed) size of workload during transmissio
    private int deadline; // tolerable deadline in ms
    @Builder.Default
    private Map<String, String> requirements = new HashMap<>(); // requirements for the task
    @Builder.Default
    private Map<String, String> properties = new HashMap<>(); // properties of the task

    public static CreateNewTaskResponseDTO fromTask(Task task) {
        return CreateNewTaskResponseDTO.builder()
                .taskUUID(task.getUuid())
                .taskMetaData(TaskMetaDataDTO.from(task.getTaskMetaData()))
                .created(task.getCreated())
                .arrived(task.getArrived())
                .sizeOfWorkload(task.getSizeOfWorkload())
                .sizeDuringTransmission(task.getSizeDuringTransmission())
                .deadline(task.getDeadline())
                .requirements(task.getRequirements())
                .properties(task.getProperties())
                .build();
    }
}
