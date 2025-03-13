package dev.pulceo.prm.dto.task;

import dev.pulceo.prm.model.task.TaskMetaData;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class TaskMetaDataDTO {

    @Builder.Default
    private String callbackProtocol = "";
    @Builder.Default
    private String callbackEndpoint = "";
    @Builder.Default
    private String destinationApplication = "";
    @Builder.Default
    private String destinationApplicationComponent = "";
    @Builder.Default
    private String destinationApplicationComponentProtocol = ""; // statically generated, never changed, e.g., http
    @Builder.Default
    private String destinationApplicationComponentEndpoint = ""; // statically generated, never changed, e.g., /api/test

    public static TaskMetaDataDTO from(TaskMetaData taskMetaData) {
        return TaskMetaDataDTO.builder()
                .callbackProtocol(taskMetaData.getCallbackProtocol())
                .callbackEndpoint(taskMetaData.getCallbackEndpoint())
                .destinationApplication(taskMetaData.getDestinationApplication())
                .destinationApplicationComponent(taskMetaData.getDestinationApplicationComponent())
                .destinationApplicationComponentProtocol(taskMetaData.getDestinationApplicationComponentProtocol())
                .destinationApplicationComponentEndpoint(taskMetaData.getDestinationApplicationComponentEndpoint())
                .build();
    }

}
