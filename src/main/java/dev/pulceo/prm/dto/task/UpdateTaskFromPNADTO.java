package dev.pulceo.prm.dto.task;

import dev.pulceo.prm.model.task.TaskStatus;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.sql.Timestamp;

@Data
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class UpdateTaskFromPNADTO {

    private String remoteTaskUUID;
    private TaskStatus newTaskStatus;
    private String modifiedBy; // remoteNodeUUID
    private Timestamp modifiedOn; // timestamp where task is modified on device

}
