package dev.pulceo.prm.api.dto.metricexports;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
public class MetricExportDTO {

    private UUID metricExportUUID;
    private MetricType metricType;
    private long numberOfRecords;
    private String url;
    private MetricExportState metricExportState;
    
}
