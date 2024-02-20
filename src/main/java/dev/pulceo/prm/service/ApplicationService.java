package dev.pulceo.prm.service;

import dev.pulceo.prm.exception.ApplicationServiceException;
import dev.pulceo.prm.model.application.Application;
import dev.pulceo.prm.repository.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;

    @Autowired
    public ApplicationService(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    public Application createApplication(Application application) throws ApplicationServiceException {
        if (this.isApplicationAlreadyExisting(application.getName())) {
            throw new ApplicationServiceException(String.format("Application %s already exists", application.getName()));
        }

        Application persistedApplication = this.applicationRepository.save(application);

        // TODO: check application components

        return persistedApplication;
    }

    private boolean isApplicationAlreadyExisting(String name) {
        return this.applicationRepository.findByName(name).isPresent();
    }
}
