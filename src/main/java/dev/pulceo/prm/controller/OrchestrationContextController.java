package dev.pulceo.prm.controller;

import dev.pulceo.prm.dto.orchestration.OrchestrationContextDTO;
import dev.pulceo.prm.exception.OrchestrationServiceException;
import dev.pulceo.prm.model.orchestration.OrchestrationContext;
import dev.pulceo.prm.service.OrchestrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orchestration-context")
public class OrchestrationContextController {

    private final OrchestrationService orchestrationService;

    @Autowired
    public OrchestrationContextController(OrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @GetMapping("")
    public ResponseEntity<OrchestrationContextDTO> getOrchestrationContext() throws OrchestrationServiceException {
        OrchestrationContext orchestrationContext = this.orchestrationService.getOrCreateOrchestrationContext();
        return ResponseEntity.ok(OrchestrationContextDTO.fromOrchestrationContext(orchestrationContext));
    }


}
