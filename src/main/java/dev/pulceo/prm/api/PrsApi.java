package dev.pulceo.prm.api;

import dev.pulceo.prm.api.dto.report.GenerateReportRequestDTO;
import dev.pulceo.prm.api.exception.PrsApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
public class PrsApi {

    private final Logger logger = LoggerFactory.getLogger(PrsApi.class);
    @Value("${prs.endpoint}")
    private String prsEndpoint;
    private final WebClient webClient;
    private final String PRS_REPORTS_API_BASE_PATH = "/api/v1/reports";

    public PrsApi(WebClient webClient) {
        this.webClient = webClient;
    }

    public void generateOrchestrationReport(GenerateReportRequestDTO generateReportRequestDTO) {
        this.webClient
                .post()
                .uri(this.prsEndpoint + "/" + this.PRS_REPORTS_API_BASE_PATH)
                .bodyValue(generateReportRequestDTO)
                .retrieve()
                .bodyToMono(Void.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(10)))
                .doOnSuccess(response -> {
                    this.logger.info("Successfully started orchestration report generation on PRS");
                })
                .onErrorResume(e -> {
                    this.logger.error("Failed to start orchestration report generation on PRS: {}", e.getMessage());
                    return Mono.empty();
                })
                .subscribe();
    }

    public void checkHealth() {
        this.webClient
                .get()
                .uri(this.prsEndpoint + "/prs/health")
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(response -> {
                    this.logger.info("PRS health check successful");
                })
                .onErrorResume(e -> {
                    this.logger.error("PRS health check failed: {}", e.getMessage());
                    throw new RuntimeException(new PrsApiException("PRS health check failed", e));
                })
                .subscribe();
    }

}
