package dev.pulceo.prm.dto.node;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
public class NodePropertiesDTO {

    @NotBlank(message="Name is required!")
    private String name;

    @NotNull(message = "Node type is required!")
    private NodeType type = NodeType.EDGE;

    @Min(1)
    @Max(16)
    private int layer = 1;

    @NotNull(message="Node role is required!")
    private NodeRole role = NodeRole.WORKLOAD;

    @NotNull
    private String nodeLocationCountry = "";

    @NotNull
    private String nodeLocationCity = "";

    @Min(-180)
    @Max(180)
    private double nodeLocationLongitude = 0.000000;

    @Min(-90)
    @Max(90)
    private double nodeLocationLatitude = 0.000000;

}
