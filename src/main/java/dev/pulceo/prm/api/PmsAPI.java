package dev.pulceo.prm.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class PmsAPI {

    private final Logger logger = LoggerFactory.getLogger(PmsAPI.class);
    @Value("${pms.endpoint}")
    private String pmsEndpoint;
    private final WebClient webClient;
    private final String PMS_METRIC_EXPORTS_API_BASE_PATH = "/api/v1/metric-exports";
    private final String PMS_ORCHESTRATION_CONTEXT_API_BASE_PATH = "/api/v1/orchestration-context";
    @Value("${webclient.scheme}")
    private String webClientScheme;

    @Autowired
    public PmsAPI(WebClient webClient) {
        this.webClient = webClient;
    }

    public void resetOrchestrationContext() {
        this.logger.info("Reset orchestration context on PMS");
        this.webClient
                .post()
                .uri(this.pmsEndpoint + this.PMS_ORCHESTRATION_CONTEXT_API_BASE_PATH + "/reset")
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(response -> {
                    this.logger.info("Successfully reset orchestration context on PMS");
                })
                .onErrorResume(e -> {
                    this.logger.error("Failed to reset orchestration context on PMS: {}", e.getMessage());
                    return Mono.empty();
                })
                .block();
    }
}
