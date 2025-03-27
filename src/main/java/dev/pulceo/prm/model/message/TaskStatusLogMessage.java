package dev.pulceo.prm.model.message;

import dev.pulceo.prm.model.task.TaskStatusLog;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@SuperBuilder
public class TaskStatusLogMessage implements Serializable {

    private String taskUUID;
    private String timestamp;
    private String previousStatus;
    private String newStatus;
    private String modifiedOn;
    private String modifiedBy;
    private String modifiedById;
    private String previousStateOfTask;
    private String newStateOfTask;
    private String taskSchedulingUUID;
    private String comment;

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
