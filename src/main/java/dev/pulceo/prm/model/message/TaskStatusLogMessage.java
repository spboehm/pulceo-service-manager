package dev.pulceo.prm.model.message;

import dev.pulceo.prm.model.task.TaskStatusLog;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@SuperBuilder
public class TaskStatusLogMessage implements Serializable {

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

    public static TaskStatusLogMessage fromTaskStatusLog(TaskStatusLog taskStatusLog) {
        return TaskStatusLogMessage.builder()
                .taskUUID(taskStatusLog.getTask().getUuid().toString())
                .timestamp(taskStatusLog.getTimestamp().toString())
                .previousStatus(taskStatusLog.getPreviousStatus().name())
                .newStatus(taskStatusLog.getNewStatus().name())
                .modifiedOn(taskStatusLog.getModifiedOn().toString())
                .modifiedBy(taskStatusLog.getModifiedBy())
                .modifiedById(taskStatusLog.getModifiedById())
                .previousStateOfTask(taskStatusLog.getPreviousStateOfTask())
                .newStateOfTask(taskStatusLog.getNewStateOfTask())
                .taskSchedulingUUID(taskStatusLog.getTaskScheduling().getUuid().toString())
                .comment(taskStatusLog.getComment())
                .build();
    }

}
