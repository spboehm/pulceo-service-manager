package dev.pulceo.prm.api.dto.report;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
public class GenerateReportRequestDTO {

    private UUID orchestrationUUID;

}
