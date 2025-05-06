package dev.pulceo.prm.api;

import dev.pulceo.prm.api.dto.metricexports.MetricExportDTO;
import dev.pulceo.prm.api.dto.metricexports.MetricExportRequestDTO;
import dev.pulceo.prm.api.dto.metricexports.MetricExportState;
import dev.pulceo.prm.api.dto.metricexports.MetricType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;

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

    public byte[] getAllCpuUtilizationRaw() {
        // create metric export request
        MetricExportDTO metricExportDTO = this.createMetricExportRequest(MetricExportRequestDTO.builder()
                .metricType(MetricType.CPU_UTIL)
                .build());

        // poll the current state of export
        this.logger.info("Retrieve state of metric export request");
        MetricExportDTO pendingMetricExportDTO =
                this.webClient.get()
                        .uri(this.pmsEndpoint + this.PMS_METRIC_EXPORTS_API_BASE_PATH + "/" + metricExportDTO.getMetricExportUUID())
                        .retrieve()
                        .bodyToMono(MetricExportDTO.class)
                        .retryWhen(Retry.backoff(3, Duration.ofSeconds(10)))
                        .doOnSuccess(response -> {
                            this.logger.info("Successfully polled metric export request");
                        })
                        .onErrorResume(e -> {
                            this.logger.error("Failed to poll metric export request: {}", e.getMessage());
                            return Mono.empty();
                        })
                        .block();

        while (pendingMetricExportDTO.getMetricExportState() != MetricExportState.COMPLETED) {
            this.logger.info("Waiting for metric export to complete...");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                this.logger.error("Thread interrupted: {}", e.getMessage());
            }
            pendingMetricExportDTO =
                    this.webClient.get()
                            .uri(this.pmsEndpoint + this.PMS_METRIC_EXPORTS_API_BASE_PATH + "/" + metricExportDTO.getMetricExportUUID())
                            .retrieve()
                            .bodyToMono(MetricExportDTO.class)
                            .retryWhen(Retry.backoff(3, Duration.ofSeconds(10)))
                            .doOnSuccess(response -> {
                                this.logger.info("Successfully polled metric export request");
                            })
                            .onErrorResume(e -> {
                                this.logger.error("Failed to poll metric export request: {}", e.getMessage());
                                return Mono.empty();
                            })
                            .block();
        }

        // download the file
        this.logger.info("Metric export completed, downloading file...");
        byte[] file = webClient.get()
                .uri(this.pmsEndpoint + this.PMS_METRIC_EXPORTS_API_BASE_PATH + "/" + metricExportDTO.getMetricExportUUID() + "/blobs/" + pendingMetricExportDTO.getUrl().substring(pendingMetricExportDTO.getUrl().lastIndexOf("/") + 1))
                .retrieve()
                .bodyToMono(byte[].class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(10)))
                .doOnSuccess(response -> {
                    this.logger.info("Successfully downloaded metric export file");
                })
                .onErrorResume(e -> {
                    this.logger.error("Failed to download metric export file: {}", e.getMessage());
                    return Mono.empty();
                })
                .block();

        return new byte[0];
    }

    private MetricExportDTO createMetricExportRequest(MetricExportRequestDTO metricExportRequestDTO) {
        this.logger.error("Create metric export request");
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
