package dev.pulceo.prm.dto.node;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class NodePropertiesDTO {

    @NotBlank(message = "Name is required!")
    private String name;

    @NotNull(message = "Node type is required!")
    @Builder.Default
    private NodeType type = NodeType.EDGE;

    @Min(1)
    @Max(16)
    @Builder.Default
    private int layer = 1;

    @NotNull(message = "Node role is required!")
    @Builder.Default
    private NodeRole role = NodeRole.WORKLOAD;

    @NotNull
    @Builder.Default
    private String nodeLocationCountry = "";

    @NotNull
    @Builder.Default
    private String nodeLocationCity = "";

    @Min(-180)
    @Max(180)
    @Builder.Default
    private double nodeLocationLongitude = 0.000000;

    @Min(-90)
    @Max(90)
    @Builder.Default
    private double nodeLocationLatitude = 0.000000;

}
