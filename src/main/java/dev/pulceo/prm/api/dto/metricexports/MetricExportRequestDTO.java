package dev.pulceo.prm.api.dto.metricexports;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class MetricExportRequestDTO {

    @NotNull
    private MetricType metricType;

}
