package dev.pulceo.prm.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PmsAPI {

    private final Logger logger = LoggerFactory.getLogger(PmsAPI.class);
    @Value("${pms.endpoint}")
    private String pmsEndpoint;
    private final WebClient webClient;
    private final String PMS_METRIC_EXPORTS_API_BASE_PATH = "/api/v1/metric-exports";
    @Value("${webclient.scheme}")
    private String webClientScheme;

    @Autowired
    public PmsAPI(WebClient webClient) {
        this.webClient = webClient;
    }
}
