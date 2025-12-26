package dev.pulceo.prm.model.orchestration;

import dev.pulceo.prm.dto.orchestration.OrchestrationStatusDTO;

public enum OrchestrationStatus {

    NEW, RUNNING, COMPLETED;

    public static OrchestrationStatus fromOrchestrationStatusDTO(OrchestrationStatusDTO dto) {
        if (dto == null || dto.getOrchestrationStatus() == null) {
            return null;
        }
        return OrchestrationStatus.valueOf(dto.getOrchestrationStatus().name());
    }
}
