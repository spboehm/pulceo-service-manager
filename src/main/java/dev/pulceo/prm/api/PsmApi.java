package dev.pulceo.prm.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;

@Component
public class PsmApi {

    private final Logger logger = LoggerFactory.getLogger(PsmApi.class);
    // TODO: Scheme
    // TODO: host (localhost)
    // TODO: port (8080),
    @Value("${webclient.scheme}://localhost:${pms.endpoint}")
    private String psmEndpoint; // Replace with actual PSM endpoint
    private final String PSM_APPLICATIONS_API_BASE_PATH = "/api/v1/applications";
    private final WebClient webClient;

    private final ApiUtils apiUtils;

    @Autowired
    public PsmApi(WebClient webClient, ApiUtils apiUtils) {
        this.webClient = webClient;
        this.apiUtils = apiUtils;
    }

    public byte[] getAllApplicationsRaw() {
        return this.apiUtils.getRaw(URI.create(this.psmEndpoint + PSM_APPLICATIONS_API_BASE_PATH));
    }

}
