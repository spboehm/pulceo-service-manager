package dev.pulceo.prm.dto.task;

import dev.pulceo.prm.model.task.TaskScheduling;
import dev.pulceo.prm.model.task.TaskStatus;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.Map;

@Data
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@ToString
public class TaskSchedulingDTO {

    @Builder.Default
    private String taskId = ""; // global task id
    @Builder.Default
    private String nodeId = ""; // global node id
    @Builder.Default
    private String applicationId = ""; // global application id
    @Builder.Default
    private String applicationComponentId = ""; // global application component id
    @Builder.Default
    private TaskStatus status = TaskStatus.NONE; // task status
    @Builder.Default
    private Map<String, String> properties = new HashMap<>(); // properties of the task

    public static TaskSchedulingDTO from(TaskScheduling taskScheduling) {
        return TaskSchedulingDTO.builder()
                .taskId(taskScheduling.getGlobalTaskUUID())
                .nodeId(taskScheduling.getNodeId())
                .applicationId(taskScheduling.getApplicationId())
                .applicationComponentId(taskScheduling.getApplicationComponentId())
                .status(taskScheduling.getStatus())
                .properties(taskScheduling.getProperties())
                .build();
    }

}
