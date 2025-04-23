package dev.pulceo.prm.controller;

import dev.pulceo.prm.exception.OrchestrationServiceException;
import dev.pulceo.prm.model.orchestration.Orchestration;
import dev.pulceo.prm.service.OrchestrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/orchestrations")
public class OrchestrationController {

    private final OrchestrationService orchestrationService;

    @Autowired
    public OrchestrationController(OrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @PostMapping
    public ResponseEntity<Orchestration> createOrchestration(@RequestBody Orchestration orchestration) {
        try {
            Orchestration createdOrchestration = orchestrationService.createOrchestration(orchestration);
            return ResponseEntity.ok(createdOrchestration);
        } catch (OrchestrationServiceException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/{name}")
    public ResponseEntity<Orchestration> readOrchestrationByName(@PathVariable String name) {
        Optional<Orchestration> orchestration = orchestrationService.readOrchestrationByName(name);
        return orchestration.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{name}")
    public ResponseEntity<Orchestration> updateOrchestration(@PathVariable String name, @RequestBody Orchestration updatedOrchestration) {
        Optional<Orchestration> existingOrchestration = orchestrationService.readOrchestrationByName(name);
        if (existingOrchestration.isPresent()) {
            Orchestration orchestration = existingOrchestration.get();
            orchestration.setName(updatedOrchestration.getName());
            orchestration.setDescription(updatedOrchestration.getDescription());
            try {
                Orchestration savedOrchestration = orchestrationService.createOrchestration(orchestration);
                return ResponseEntity.ok(savedOrchestration);
            } catch (OrchestrationServiceException e) {
                return ResponseEntity.badRequest().body(null);
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> deleteOrchestrationByName(@PathVariable String name) {
        orchestrationService.deleteOrchestrationByName(name);
        return ResponseEntity.noContent().build();
    }
}
