package dev.pulceo.prm.model.message;

import dev.pulceo.prm.model.task.TaskStatusLog;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@SuperBuilder
public class TaskStatusLogMessage implements Serializable {

    @Builder.Default
    private long taskSequenceNumber = 0; // sequence number of the task
    @Builder.Default
    private String taskUUID = "";
    @Builder.Default
    private String timestamp = "";
    @Builder.Default
    private String previousStatus = "";
    @Builder.Default
    private String newStatus = "";
    @Builder.Default
    private String modifiedOn = "";
    @Builder.Default
    private String modifiedBy = "";
    @Builder.Default
    private String modifiedById = "";
    @Builder.Default
    private String previousStateOfTask = "";
    @Builder.Default
    private String newStateOfTask = "";
    @Builder.Default
    private String taskSchedulingUUID = "";
    @Builder.Default
    private String comment = "";
    @Builder.Default
    private Map<String, String> properties = new HashMap<>();

    public static TaskStatusLogMessage fromTaskStatusLog(long taskSequenceNumber, String taskUUID, TaskStatusLog taskStatusLog, Map<String, String> properties) {
        return TaskStatusLogMessage.builder()
                .taskSequenceNumber(taskSequenceNumber)
                .taskUUID(taskUUID)
                .timestamp(taskStatusLog.getTimestamp().toString())
                .previousStatus(null)
                .newStatus(taskStatusLog.getNewStatus().name())
                .modifiedOn(taskStatusLog.getModifiedOn().toString())
                .modifiedBy(taskStatusLog.getModifiedBy())
                .modifiedById(taskStatusLog.getModifiedById())
                .previousStateOfTask(taskStatusLog.getPreviousStateOfTask())
                .newStateOfTask(taskStatusLog.getNewStateOfTask())
                .taskSchedulingUUID("")
                .comment(taskStatusLog.getComment())
                .properties(properties)
                .build();
    }

}
