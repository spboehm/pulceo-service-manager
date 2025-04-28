package dev.pulceo.prm.controller;

import dev.pulceo.prm.dto.application.ApplicationDTO;
import dev.pulceo.prm.dto.application.CreateNewApplicationDTO;
import dev.pulceo.prm.exception.ApplicationServiceException;
import dev.pulceo.prm.model.application.Application;
import dev.pulceo.prm.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    @Autowired
    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationDTO> readApplicationById(@PathVariable String id) {
        Optional<Application> application = this.resolveApplication(id);
        if (application.isEmpty()) {
            return ResponseEntity.status(404).build();
        }
        return ResponseEntity.status(200).body(ApplicationDTO.fromApplication(application.get()));
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
    public ResponseEntity<ApplicationDTO> createNewApplication(@Valid @RequestBody CreateNewApplicationDTO createNewApplicationDTO) throws ApplicationServiceException, InterruptedException {
        Application application = applicationService.createPreliminaryApplication(Application.fromCreateNewApplicationDTO(createNewApplicationDTO));
        this.applicationService.createApplicationAsync(application);
        return ResponseEntity.status(201).body(ApplicationDTO.fromApplication(application));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void deleteApplication(@PathVariable String id) throws ApplicationServiceException, InterruptedException {
        Optional<Application> application = this.resolveApplication(id);
        if (application.isEmpty()) {
            throw new ApplicationServiceException("Application %s does not exist!".formatted(id));
        }
        this.applicationService.deleteApplication(application.get().getUuid());
    }

    private Optional<Application> resolveApplication(String id) {
        Optional<Application> application;
        // TODO: add resolve to name here, heck if UUID
        if (checkIfUUID(id)) {
            application = this.applicationService.readApplicationByUUID(UUID.fromString(id));
        } else {
            application = this.applicationService.readApplicationByName(id);
        }
        return application;
    }

    private static boolean checkIfUUID(String uuid) {
        String uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
        return uuid.matches(uuidRegex);
    }

    // TODO: add exception handler
    @ExceptionHandler(value = ApplicationServiceException.class)
    public ResponseEntity<CustomErrorResponse> handleCloudRegistrationException(ApplicationServiceException nodeServiceException) {
        CustomErrorResponse error = new CustomErrorResponse("BAD_REQUEST", nodeServiceException.getMessage());
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setErrorMsg(nodeServiceException.getMessage());
        error.setTimestamp(LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = InterruptedException.class)
    public ResponseEntity<CustomErrorResponse> handleCloudRegistrationException(InterruptedException interruptedException) {
        CustomErrorResponse error = new CustomErrorResponse("BAD_REQUEST", interruptedException.getMessage());
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setErrorMsg(interruptedException.getMessage());
        error.setTimestamp(LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

}
