package dev.pulceo.prm.dto.task;

import dev.pulceo.prm.model.task.Task;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class TaskDTO {

    private UUID taskUUID;
    private TaskMetaDataDTO taskMetaData;
    private TaskSchedulingDTO taskScheduling;
    @Builder.Default
    private Timestamp created = Timestamp.valueOf(LocalDateTime.now()); // timestamp where task is created on device
    private String createdBy; // user who created the task
    private String arrivedAt; // user who has arrived the task at the servers
    @Builder.Default
    private Timestamp arrived = Timestamp.valueOf(LocalDateTime.now()); // timestamp where task arrived on device
    private long sizeOfWorkload; // size of the input data
    private long sizeDuringTransmission; // (compressed) size of workload during transmissio
    private int deadline; // tolerable deadline in ms

    public static TaskDTO fromTask(Task task) {
        return TaskDTO.builder()
                .taskUUID(task.getUuid())
                .taskMetaData(TaskMetaDataDTO.from(task.getTaskMetaData()))
                .taskScheduling(TaskSchedulingDTO.from(task.getTaskScheduling()))
                .created(task.getCreated())
                .createdBy(task.getCreatedBy())
                .arrived(task.getArrived())
                .arrivedAt(task.getArrivedAt())
                .sizeOfWorkload(task.getSizeOfWorkload())
                .sizeDuringTransmission(task.getSizeDuringTransmission())
                .deadline(task.getDeadline())
                .build();
    }
}
