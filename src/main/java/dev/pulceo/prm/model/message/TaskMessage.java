package dev.pulceo.prm.model.message;

import dev.pulceo.prm.model.task.Task;
import dev.pulceo.prm.model.task.TaskStatus;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@SuperBuilder
public class TaskMessage implements Serializable {

    @Builder.Default
    private String taskUUID = ""; // UUID of the task
    @Builder.Default
    private long taskSequenceNumber = 0; // sequence number of the task
    @Builder.Default
    private TaskStatus taskStatus = TaskStatus.NONE;
    @Builder.Default
    private Timestamp created = Timestamp.valueOf(LocalDateTime.now()); // timestamp where task is created on device
    @Builder.Default
    private String createdBy = ""; // user who created the task
    @Builder.Default
    private Timestamp arrived = Timestamp.valueOf(LocalDateTime.now()); // timestamp where task has arrived at the servers
    @Builder.Default
    private String arrivedAt = "psm"; // user who has arrived the task at the servers
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

    public static TaskMessage fromTask(Task task) {
        return TaskMessage.builder()
                .taskUUID(task.getUuid().toString())
                .taskSequenceNumber(task.getTaskSequenceNumber())
                .taskStatus(task.getTaskScheduling().getStatus())
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

    public static TaskMessage fromTask(Task task, TaskStatus taskStatus) {
        return TaskMessage.builder()
                .taskUUID(task.getUuid().toString())
                .taskSequenceNumber(task.getTaskSequenceNumber())
                .taskStatus(taskStatus)
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
