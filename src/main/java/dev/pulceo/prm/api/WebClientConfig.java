package dev.pulceo.prm.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    // generic web client configuration
    @Bean
    public WebClient webClient() {
        return WebClient.create();
    }

}
