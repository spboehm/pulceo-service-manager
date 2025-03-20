package dev.pulceo.prm.dto.task;

import dev.pulceo.prm.model.task.TaskStatus;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.sql.Timestamp;

@Data
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@ToString
public class UpdateTaskFromPNADTO {

    private String globalTaskUUID; // based on the one from PSM
    private String remoteTaskUUID;
    private TaskStatus newTaskStatus;
    private String modifiedByRemoteNodeUUID; // always the pna remote node uuid
    private Timestamp modifiedOn; // timestamp where task is modified on device

}
