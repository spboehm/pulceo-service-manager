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
    private String callbackProtocol = ""; // statically generated, never changed
    @Builder.Default
    private String callbackEndpoint = ""; // statically generated, never changed
    @Builder.Default
    private String destinationApplication = ""; // statically generated, never changed
    @Builder.Default
    private String destinationApplicationComponent = ""; // statically generated, never changed
    @Builder.Default
    private String destinationApplicationComponentProtocol = ""; // statically generated, never changed, e.g., http
    @Builder.Default
    private String destinationApplicationComponentEndpoint = ""; // statically generated, never changed, e.g., /api/test
}
