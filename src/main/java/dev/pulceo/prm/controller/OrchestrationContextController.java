package dev.pulceo.prm.controller;

import dev.pulceo.prm.dto.orchestration.OrchestrationContextDTO;
import dev.pulceo.prm.exception.OrchestrationServiceException;
import dev.pulceo.prm.model.orchestration.OrchestrationContext;
import dev.pulceo.prm.service.ApplicationService;
import dev.pulceo.prm.service.OrchestrationService;
import dev.pulceo.prm.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

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
    public void deleteOrchestrationContext() throws OrchestrationServiceException {
        this.applicationService.reset();
        this.taskService.reset();
        this.orchestrationService.reset();
    }

    @ExceptionHandler(value = OrchestrationServiceException.class)
    public ResponseEntity<CustomErrorResponse> handleCloudRegistrationException(OrchestrationServiceException orchestrationServiceException) {
        CustomErrorResponse error = new CustomErrorResponse("BAD_REQUEST", orchestrationServiceException.getMessage());
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setErrorMsg(orchestrationServiceException.getMessage());
        error.setTimestamp(LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

}
