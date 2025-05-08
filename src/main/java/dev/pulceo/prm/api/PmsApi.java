package dev.pulceo.prm.api;

import dev.pulceo.prm.api.dto.metricexports.MetricExportDTO;
import dev.pulceo.prm.api.dto.metricexports.MetricExportRequestDTO;
import dev.pulceo.prm.api.dto.metricexports.MetricExportState;
import dev.pulceo.prm.api.dto.metricexports.MetricType;
import dev.pulceo.prm.api.exception.PmsApiException;
import dev.pulceo.prm.api.exception.ResourceNotReadyException;
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

    @Autowired
    public PmsApi(WebClient webClient, ApiUtils apiUtils) {
        this.webClient = webClient;
        this.apiUtils = apiUtils;
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

    public void requestMetric(UUID orchestrationUuid, MetricType metricType) throws PmsApiException {
        // create metric export request
        MetricExportDTO metricExportDTO = this.createMetricExportRequest(MetricExportRequestDTO.builder()
                .metricType(metricType)
                .build());

        // wait for completion
        this.logger.info("Waiting for metric export with uuid={} to complete...", metricExportDTO.getMetricExportUUID());
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
            this.logger.info("Requested file exists");
        } else {
            this.logger.error("Failed to retrieve requested file");
            throw new PmsApiException("Failed to retrieve requested file");
        }
    }

    private boolean checkIfRequestedFileExists(UUID orchestrationUuid, MetricType metricType) {
        this.logger.info("Check if requested file exists...");
        Path filePath = Path.of(this.pulceoDataDir, "raw", orchestrationUuid.toString(), metricType + ".csv");
        return Files.exists(filePath);
    }

    private MetricExportDTO createMetricExportRequest(MetricExportRequestDTO metricExportRequestDTO) {
        this.logger.info("Create metric export request");
        return webClient.post()
                .uri(this.pmsEndpoint + this.PMS_METRIC_EXPORTS_API_BASE_PATH)
                .bodyValue(metricExportRequestDTO)
                .retrieve()
                .bodyToMono(MetricExportDTO.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(10)))
                .doOnSuccess(response -> {
                    this.logger.info("Successfully created metric export request");
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

}
