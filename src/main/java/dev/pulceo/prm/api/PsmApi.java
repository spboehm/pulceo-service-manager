package dev.pulceo.prm.api;

import dev.pulceo.prm.api.dto.resource.PsmResources;
import dev.pulceo.prm.api.exception.PsmApiException;
import dev.pulceo.prm.util.FileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.nio.file.Path;
import java.util.UUID;

@Component
public class PsmApi {

    private final Logger logger = LoggerFactory.getLogger(PsmApi.class);
    // TODO: Scheme
    // TODO: host (localhost)
    // TODO: port (8080),
    @Value("${psm.endpoint}")
    private String psmEndpoint; // Replace with actual PSM endpoint
    private final String PSM_APPLICATIONS_API_BASE_PATH = "/api/v1/applications";
    private final WebClient webClient;

    @Value("${pulceo.data.dir}")
    private String pulceoDataDir;

    private final ApiUtils apiUtils;
    private final FileManager fileManager;

    @Autowired
    public PsmApi(WebClient webClient, ApiUtils apiUtils, FileManager fileManager) {
        this.webClient = webClient;
        this.apiUtils = apiUtils;
        this.fileManager = fileManager;
    }

    public byte[] getAllApplicationsRaw() {
        return this.apiUtils.getRaw(URI.create(this.psmEndpoint + PSM_APPLICATIONS_API_BASE_PATH));
    }

    public void requestResource(UUID orchestrationUuid, PsmResources resourceType, boolean cleanUp) {
        this.logger.info("Request static orchestration data with orchestrationUuid={} resourceType={} and cleanUp={}", orchestrationUuid, resourceType, cleanUp);
        Path requestedFilePath = Path.of(this.pulceoDataDir, "raw", orchestrationUuid.toString(), resourceType + ".json");
        boolean requestedFileExists = this.fileManager.checkIfRequestedFileExists(requestedFilePath);

        if (requestedFileExists) {
            if (!cleanUp) {
                return;
            }
        }
        this.requestResource(orchestrationUuid, resourceType);
    }

    public void requestResource(UUID orchestrationUuid, PsmResources resourceType) {
        switch (resourceType) {
            case APPLICATIONS -> requestApplications(orchestrationUuid);
            default -> throw new PsmApiException("Resource type not supported: " + resourceType);
        }
    }

    private void requestApplications(UUID orchestrationUuid) {
        byte[] applicationsRaw = this.getAllApplicationsRaw();
        this.fileManager.saveAsJson(applicationsRaw, "raw", orchestrationUuid.toString(), "APPLICATIONS.json");
    }

    public void collectStaticOrchestrationData(UUID orchestrationUuid, boolean cleanUp) {
        this.requestResource(orchestrationUuid, PsmResources.APPLICATIONS, cleanUp);
    }

}
