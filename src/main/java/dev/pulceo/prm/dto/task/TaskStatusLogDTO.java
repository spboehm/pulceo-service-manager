package dev.pulceo.prm.dto.task;

import dev.pulceo.prm.model.task.TaskStatus;
import dev.pulceo.prm.model.task.TaskStatusLog;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class TaskStatusLogDTO {

    @Builder.Default
    private String timestamp = LocalDateTime.now().toString();
    private TaskStatus previousStatus;
    private TaskStatus newStatus;
    @Builder.Default
    private String modifiedBy = "psm";
    @Builder.Default
    private String modifiedById = "";
    @Builder.Default
    private String modifiedOn = "";
    @Builder.Default
    private String previousStateOfTask = "";
    @Builder.Default
    private String newStateOfTask = "";
    @Builder.Default
    private String comment = "";

    public static TaskStatusLogDTO from(TaskStatusLog taskStatusLog) {
        return TaskStatusLogDTO.builder()
                .timestamp(taskStatusLog.getTimestamp().toString())
                .previousStatus(taskStatusLog.getPreviousStatus())
                .newStatus(taskStatusLog.getNewStatus())
                .modifiedOn(taskStatusLog.getModifiedOn().toString())
                .modifiedBy(taskStatusLog.getModifiedBy())
                .modifiedById(taskStatusLog.getModifiedById())
                .previousStateOfTask(taskStatusLog.getPreviousStateOfTask())
                .newStateOfTask(taskStatusLog.getNewStateOfTask())
                .comment(taskStatusLog.getComment())
                .build();
    }

}
