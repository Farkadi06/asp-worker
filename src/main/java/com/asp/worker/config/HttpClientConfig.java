package com.asp.worker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class HttpClientConfig {

    @Value("${ASP_CORE_BASE_URL:http://localhost:8080}")
    private String coreBaseUrl;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .baseUrl(coreBaseUrl)
            .build();
    }
}

