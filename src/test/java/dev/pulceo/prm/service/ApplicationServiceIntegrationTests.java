package dev.pulceo.prm.service;

import dev.pulceo.prm.exception.ApplicationServiceException;
import dev.pulceo.prm.model.application.Application;
import dev.pulceo.prm.repository.ApplicationRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ApplicationServiceIntegrationTests {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ApplicationRepository applicationRepository;

    @BeforeEach
    public void setup() {
        applicationRepository.deleteAll();
    }

    @AfterAll
    public void tearDown() {
        // applicationRepository.deleteAll();
    }

    @Test
    public void testCreateApplication() throws ApplicationServiceException {
        // given
        Application application = Application.builder()
                .name("test-application")
                .applicationComponents(new ArrayList<>())
                .build();

        // when
        Application createdApplication = applicationService.createApplication(application);

        // then
        assertEquals(application, createdApplication);
    }

}
