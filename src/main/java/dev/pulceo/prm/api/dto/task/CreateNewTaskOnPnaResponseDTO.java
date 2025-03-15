package dev.pulceo.prm.api.dto.task;

import dev.pulceo.prm.model.task.TaskStatus;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@ToString
public class CreateNewTaskOnPnaResponseDTO {

    private UUID remoteNodeUUID;
    private UUID remoteTaskUUID;
    private TaskStatus status;

}
