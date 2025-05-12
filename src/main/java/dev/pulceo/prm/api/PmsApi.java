package dev.pulceo.prm.api;

import dev.pulceo.prm.api.dto.metricexports.MetricExportDTO;
import dev.pulceo.prm.api.dto.metricexports.MetricExportRequestDTO;
import dev.pulceo.prm.api.dto.metricexports.MetricExportState;
import dev.pulceo.prm.api.dto.metricexports.MetricType;
import dev.pulceo.prm.api.dto.resource.PmsResources;
import dev.pulceo.prm.api.exception.PmsApiException;
import dev.pulceo.prm.api.exception.ResourceNotReadyException;
import dev.pulceo.prm.util.FileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

@Component
public class PmsApi {

    private final Logger logger = LoggerFactory.getLogger(PmsApi.class);
    @Value("${pms.endpoint}")
    private String pmsEndpoint;
    private final WebClient webClient;
    private final String PMS_METRIC_REQUESTS_API_BASE_PATH = "/api/v1/metric-requests";
    private final String PMS_METRIC_EXPORTS_API_BASE_PATH = "/api/v1/metric-exports";
    private final String PMS_ORCHESTRATION_CONTEXT_API_BASE_PATH = "/api/v1/orchestration-context";
    @Value("${webclient.scheme}")
    private String webClientScheme;
    private final ApiUtils apiUtils;
    // TODO: add directory where files are stored
    @Value("${pulceo.data.dir}")
    private String pulceoDataDir;
    private final FileManager fileManager;

    @Autowired
    public PmsApi(WebClient webClient, ApiUtils apiUtils, FileManager fileManager) {
        this.webClient = webClient;
        this.apiUtils = apiUtils;
        this.fileManager = fileManager;
    }

    public void resetOrchestrationContext() {
        this.logger.info("Reset orchestration context on PMS");
        this.webClient
                .post()
                .uri(this.pmsEndpoint + this.PMS_ORCHESTRATION_CONTEXT_API_BASE_PATH + "/reset")
                .retrieve()
                .bodyToMono(Void.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(10)))
                .doOnSuccess(response -> {
                    this.logger.info("Successfully reset orchestration context on PMS");
                })
                .onErrorResume(e -> {
                    this.logger.error("Failed to reset orchestration context on PMS: {}", e.getMessage());
                    return Mono.empty();
                })
                .subscribe();
    }

    public void requestMetric(UUID orchestrationUuid, MetricType metricType, boolean cleanUp) throws PmsApiException {
        this.logger.info("Request metric with orchestrationUuid={}, metricType={} and cleanUp={}", orchestrationUuid, metricType, cleanUp);
        boolean requestedFileExists = this.checkIfRequestedFileExists(orchestrationUuid, metricType);
        if (requestedFileExists) {
            if (!cleanUp) {
                return;
            }
        }
        this.requestMetric(orchestrationUuid, metricType);
    }

    private void requestMetric(UUID orchestrationUuid, MetricType metricType) throws PmsApiException {
        // create metric export request
        MetricExportDTO metricExportDTO = this.createMetricExportRequest(MetricExportRequestDTO.builder()
                .metricType(metricType)
                .build());

        if (metricExportDTO == null) {
            this.logger.error("Failed to create metric export request with uuid={} and metricType={}", orchestrationUuid, metricType);
            throw new PmsApiException("Failed to create metric export request with uuid=%s and metricType=%s".formatted(orchestrationUuid, metricType));
        }

        // wait for completion
        this.logger.info("Waiting for metric export with uuid={} to complete", metricExportDTO.getMetricExportUUID());
        this.webClient.get()
                .uri(this.pmsEndpoint + this.PMS_METRIC_EXPORTS_API_BASE_PATH + "/" + metricExportDTO.getMetricExportUUID())
                .retrieve()
                .bodyToMono(MetricExportDTO.class)
                .flatMap(currentMetricExportDTO -> {
                    if (currentMetricExportDTO.getMetricExportState() == MetricExportState.COMPLETED) {
                        return Mono.just(currentMetricExportDTO);
                    } else {
                        return Mono.error(new ResourceNotReadyException("Metric export with uuid=%s is not ready yet".formatted(currentMetricExportDTO.getMetricExportUUID())));
                    }
                })
                .retryWhen(Retry.backoff(5, Duration.ofSeconds(10))
                        .filter(throwable -> throwable instanceof ResourceNotReadyException))
                .doOnSuccess(completedMetricExportDTO -> {
                    this.logger.info("Successfully polled metric export request with uuid={}", completedMetricExportDTO.getMetricExportUUID());
                    if (completedMetricExportDTO.getMetricExportState() == MetricExportState.COMPLETED) {
                        this.logger.info("Metric export request with uuid={} completed", completedMetricExportDTO.getMetricExportUUID());
                    } else {
                        this.logger.info("Metric export request with uuid={} is still pending", completedMetricExportDTO.getMetricExportUUID());
                    }
                })
                .onErrorResume(e -> {
                    this.logger.error("Failed to poll metric export request: {}", e.getMessage());
                    return Mono.empty();
                })
                .block();

        if (checkIfRequestedFileExists(orchestrationUuid, metricType)) {
            this.logger.info("Requested file for orchestrationUuid={} metricType={} exists", orchestrationUuid, metricType);
        } else {
            this.logger.error("Failed to retrieve requested file orchestrationUuid={} metricType={} exists", orchestrationUuid, metricType);
            throw new PmsApiException("Failed to retrieve requested file");
        }
    }

    private boolean checkIfRequestedFileExists(UUID orchestrationUuid, MetricType metricType) {
        Path filePath = Path.of(this.pulceoDataDir, "raw", orchestrationUuid.toString(), metricType + ".csv");
        this.logger.info("Check if requested file={} exists", filePath);
        return Files.exists(filePath);
    }

    private MetricExportDTO createMetricExportRequest(MetricExportRequestDTO metricExportRequestDTO) {
        this.logger.info("Create metric export request with payload: {}", metricExportRequestDTO);
        return webClient.post()
                .uri(this.pmsEndpoint + this.PMS_METRIC_EXPORTS_API_BASE_PATH)
                .bodyValue(metricExportRequestDTO)
                .retrieve()
                .bodyToMono(MetricExportDTO.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(10)))
                .doOnSuccess(response -> {
                    this.logger.info("Successfully created metric export request with uuid={} and metricType={}", response.getMetricExportUUID(), response.getMetricType());
                })
                .onErrorResume(e -> {
                    this.logger.error("Failed to create metric export request: {}", e.getMessage());
                    return Mono.empty();
                })
                .block();
    }

    public byte[] getAllMetricRequestsRaw() {
        return this.apiUtils.getRaw(URI.create(this.pmsEndpoint + PMS_METRIC_REQUESTS_API_BASE_PATH));
    }

    public void requestResource(UUID orchestrationUuid, PmsResources resourceType, boolean cleanUp) throws PmsApiException {
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

    private void requestResource(UUID orchestrationUuid, PmsResources resourceType) throws PmsApiException {
        switch (resourceType) {
            case METRIC_REQUESTS -> requestsMetricRequests(orchestrationUuid);
            default -> throw new PmsApiException("Resource type not supported: " + resourceType);
        }
    }

    private void requestsMetricRequests(UUID orchestrationUuid) {
        byte[] metricsRequestsRaw = this.getAllMetricRequestsRaw();
        this.fileManager.saveAsJson(metricsRequestsRaw, "raw", orchestrationUuid.toString(), "METRIC_REQUESTS.json");
    }

    public void collectStaticOrchestrationData(UUID orchestrationUuid, boolean cleanUp) throws PmsApiException {
        this.requestResource(orchestrationUuid, PmsResources.METRIC_REQUESTS, cleanUp);
    }
}
