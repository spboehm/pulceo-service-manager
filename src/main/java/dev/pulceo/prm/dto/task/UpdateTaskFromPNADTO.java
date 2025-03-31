package dev.pulceo.prm.dto.task;

import dev.pulceo.prm.model.task.TaskStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
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

    @Builder.Default
    private String globalTaskUUID = ""; // based on the one from PSM
    @Builder.Default
    private String remoteTaskUUID = "";
    @Enumerated(EnumType.STRING)
    private TaskStatus newTaskStatus;
    @Builder.Default
    private String modifiedByRemoteNodeUUID = ""; // always the pna remote node uuid
    @NotNull
    private Timestamp modifiedOn; // timestamp where task is modified on device

}
