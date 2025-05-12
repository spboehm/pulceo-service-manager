package dev.pulceo.prm.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@Component
public class ApiUtils {

    Logger logger = LoggerFactory.getLogger(ApiUtils.class);

    private final WebClient webClient;

    @Autowired
    public ApiUtils(WebClient webClient) {
        this.webClient = webClient;
    }

    public byte[] getRaw(URI uri) {
        return webClient
                .get()
                .uri(uri)
                .retrieve()
                .bodyToMono(byte[].class)
                .doOnSuccess(response -> {
                    this.logger.info("Successfully retrieved data from PSM");
                })
                .onErrorResume(e -> {
                    this.logger.error("Error retrieving data from PSM", e);
                    return Mono.just("{}".getBytes(StandardCharsets.UTF_8));
                })
                .block();
    }
}
