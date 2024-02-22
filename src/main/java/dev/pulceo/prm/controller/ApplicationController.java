package dev.pulceo.prm.controller;

import dev.pulceo.prm.dto.application.ApplicationDTO;
import dev.pulceo.prm.dto.application.CreateNewApplicationComponentDTO;
import dev.pulceo.prm.dto.application.CreateNewApplicationDTO;
import dev.pulceo.prm.exception.ApplicationServiceException;
import dev.pulceo.prm.model.application.Application;
import dev.pulceo.prm.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    ApplicationService applicationService;

    @Autowired
    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<ApplicationDTO> createNewApplication(@Valid @RequestBody CreateNewApplicationDTO createNewApplicationDTO) throws ApplicationServiceException {
        Application application = applicationService.createApplication(Application.fromCreateNewApplicationDTO(createNewApplicationDTO));
        return ResponseEntity.status(201).body(ApplicationDTO.fromApplication(application));
    }

    // TODO: add exception handler


}
