package dev.pulceo.prm.dto.task;

import dev.pulceo.prm.model.task.TaskScheduling;
import dev.pulceo.prm.model.task.TaskStatus;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class TaskSchedulingDTO {

    @Builder.Default
    private String nodeId = ""; // global node id
    @Builder.Default
    private String applicationId = ""; // global application id
    @Builder.Default
    private String applicationComponentId = ""; // global application component id
    @Builder.Default
    private TaskStatus status = TaskStatus.NONE; // task status

    public static TaskSchedulingDTO from(TaskScheduling taskScheduling) {
        return TaskSchedulingDTO.builder()
                .nodeId(taskScheduling.getNodeId())
                .applicationId(taskScheduling.getApplicationId())
                .applicationComponentId(taskScheduling.getApplicationComponentId())
                .status(taskScheduling.getStatus())
                .build();
    }

}
