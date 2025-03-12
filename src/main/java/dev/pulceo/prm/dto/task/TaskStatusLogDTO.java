package dev.pulceo.prm.dto.task;

import dev.pulceo.prm.model.task.TaskStatus;
import dev.pulceo.prm.model.task.TaskStatusLog;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class TaskStatusLogDTO {

    @Builder.Default
    private Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());
    private TaskStatus previousStatus;
    private TaskStatus newStatus;
    @Builder.Default
    private String modifiedBy = "psm";
    @Builder.Default
    private String previousStateOfTask = "";
    @Builder.Default
    private String newStateOfTask = "";
    @Builder.Default
    private String comment = "";

    public static TaskStatusLogDTO from(TaskStatusLog taskStatusLog) {
        return TaskStatusLogDTO.builder()
                .timestamp(taskStatusLog.getTimestamp())
                .previousStatus(taskStatusLog.getPreviousStatus())
                .newStatus(taskStatusLog.getNewStatus())
                .modifiedBy(taskStatusLog.getModifiedBy())
                .previousStateOfTask(taskStatusLog.getPreviousStateOfTask())
                .newStateOfTask(taskStatusLog.getNewStateOfTask())
                .comment(taskStatusLog.getComment())
                .build();
    }

}
