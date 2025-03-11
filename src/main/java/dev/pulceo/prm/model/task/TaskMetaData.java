package dev.pulceo.prm.model.task;

import dev.pulceo.prm.model.BaseEntity;
import jakarta.persistence.Entity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class TaskMetaData extends BaseEntity {

    @Builder.Default
    private String callbackProtocol = "";
    @Builder.Default
    private String callbackEndpoint = "";
    @Builder.Default
    private String destinationApplication = "";
    @Builder.Default
    private String destinationApplicationComponent = "";
    
}
