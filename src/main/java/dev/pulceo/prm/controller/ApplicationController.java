package dev.pulceo.prm.controller;

import dev.pulceo.prm.dto.application.ApplicationDTO;
import dev.pulceo.prm.dto.application.CreateNewApplicationComponentDTO;
import dev.pulceo.prm.dto.application.CreateNewApplicationDTO;
import dev.pulceo.prm.exception.ApplicationServiceException;
import dev.pulceo.prm.model.application.Application;
import dev.pulceo.prm.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    ApplicationService applicationService;

    @Autowired
    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping("")
    public ResponseEntity<List<ApplicationDTO>> readAllApplications() {
        List<Application> applications = applicationService.readAllApplications();
        List<ApplicationDTO> applicationDTOs = new ArrayList<>();
        for (Application application : applications) {
            applicationDTOs.add(ApplicationDTO.fromApplication(application));
        }
        return ResponseEntity.status(200).body(applicationDTOs);
    }

    @PostMapping
    public ResponseEntity<ApplicationDTO> createNewApplication(@Valid @RequestBody CreateNewApplicationDTO createNewApplicationDTO) throws ApplicationServiceException {
        Application application = applicationService.createApplication(Application.fromCreateNewApplicationDTO(createNewApplicationDTO));
        return ResponseEntity.status(201).body(ApplicationDTO.fromApplication(application));
    }

    @DeleteMapping("/{uuid}")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void deleteApplication(@PathVariable String uuid) throws ApplicationServiceException {
        Optional<Application> application = applicationService.readApplicationByUUID(UUID.fromString(uuid));
        if (application.isEmpty()) {
            throw new ApplicationServiceException("Application with UUID %s does not exist!".formatted(uuid));
        }
        this.applicationService.deleteApplication(UUID.fromString(uuid));
    }

    // TODO: add exception handler
}
