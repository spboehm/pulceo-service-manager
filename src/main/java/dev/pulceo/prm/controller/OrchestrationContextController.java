package dev.pulceo.prm.controller;

import dev.pulceo.prm.dto.orchestration.OrchestrationContextDTO;
import dev.pulceo.prm.exception.OrchestrationServiceException;
import dev.pulceo.prm.model.orchestration.OrchestrationContext;
import dev.pulceo.prm.service.ApplicationService;
import dev.pulceo.prm.service.OrchestrationService;
import dev.pulceo.prm.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orchestration-context")
public class OrchestrationContextController {

    private final OrchestrationService orchestrationService;
    private final TaskService taskService;
    private final ApplicationService applicationService;

    @Autowired
    public OrchestrationContextController(OrchestrationService orchestrationService, TaskService taskService, ApplicationService applicationService) {
        this.orchestrationService = orchestrationService;
        this.taskService = taskService;
        this.applicationService = applicationService;
    }

    @GetMapping("")
    public ResponseEntity<OrchestrationContextDTO> getOrchestrationContext() throws OrchestrationServiceException {
        OrchestrationContext orchestrationContext = this.orchestrationService.getOrCreateOrchestrationContext();
        return ResponseEntity.ok(OrchestrationContextDTO.fromOrchestrationContext(orchestrationContext));
    }

    @PostMapping("/reset")
    public void deleteOrchestrationContext() {
        this.applicationService.reset();
        this.taskService.reset();
        this.orchestrationService.reset();
    }

}
