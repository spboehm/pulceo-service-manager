package dev.pulceo.prm.controller;

import dev.pulceo.prm.dto.orchestration.CreateNewOrchestrationDTO;
import dev.pulceo.prm.dto.orchestration.CreateNewOrchestrationResponseDTO;
import dev.pulceo.prm.dto.orchestration.OrchestrationDTO;
import dev.pulceo.prm.exception.OrchestrationServiceException;
import dev.pulceo.prm.model.orchestration.Orchestration;
import dev.pulceo.prm.service.OrchestrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orchestrations")
public class OrchestrationController {

    private final OrchestrationService orchestrationService;

    @Autowired
    public OrchestrationController(OrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @PostMapping
    public ResponseEntity<CreateNewOrchestrationResponseDTO> createNewOrchestration(@RequestBody CreateNewOrchestrationDTO createNewOrchestrationDTO) throws OrchestrationServiceException {
        Orchestration orchestration = this.orchestrationService.createOrchestration(Orchestration.fromCreateNewOrchestrationDTO(createNewOrchestrationDTO));
        return ResponseEntity.status(201).body(CreateNewOrchestrationResponseDTO.fromOrchestration(orchestration));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrchestrationDTO> readOrchestrationById(@PathVariable String id) {
        Optional<Orchestration> orchestration = this.resolveOrchestration(id);
        if (orchestration.isEmpty()) {
            return ResponseEntity.status(404).build();
        } else {
            return ResponseEntity.status(200).body(OrchestrationDTO.fromOrchestration(orchestration.get()));
        }
    }

    private Optional<Orchestration> resolveOrchestration(String id) {
        if (checkIfUUID(id)) {
            return orchestrationService.readOrchestrationByUUID(UUID.fromString(id));
        } else {
            return orchestrationService.readOrchestrationByName(id);
        }
    }

    private boolean checkIfUUID(String uuid) {
        String uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
        return uuid.matches(uuidRegex);
    }
//
//    @PutMapping("/{name}")
//    public ResponseEntity<Orchestration> updateOrchestration(@PathVariable String name, @RequestBody Orchestration updatedOrchestration) {
//        Optional<Orchestration> existingOrchestration = orchestrationService.readOrchestrationByName(name);
//        if (existingOrchestration.isPresent()) {
//            Orchestration orchestration = existingOrchestration.get();
//            orchestration.setName(updatedOrchestration.getName());
//            orchestration.setDescription(updatedOrchestration.getDescription());
//            try {
//                Orchestration savedOrchestration = orchestrationService.createOrchestration(orchestration);
//                return ResponseEntity.ok(savedOrchestration);
//            } catch (OrchestrationServiceException e) {y
//                return ResponseEntity.badRequest().body(null);
//            }
//        } else {
//            return ResponseEntity.notFound().build();
//        }
//    }
//
//    @DeleteMapping("/{name}")
//    public ResponseEntity<Void> deleteOrchestrationByName(@PathVariable String name) {
//        orchestrationService.deleteOrchestrationByName(name);
//        return ResponseEntity.noContent().build();
//    }

    @ExceptionHandler(value = OrchestrationServiceException.class)
    public ResponseEntity<CustomErrorResponse> handleCloudRegistrationException(OrchestrationServiceException orchestrationServiceException) {
        CustomErrorResponse error = new CustomErrorResponse("BAD_REQUEST", orchestrationServiceException.getMessage());
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setErrorMsg(orchestrationServiceException.getMessage());
        error.setTimestamp(LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

}
