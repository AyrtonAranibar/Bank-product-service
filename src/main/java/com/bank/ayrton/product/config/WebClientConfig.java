package com.bank.ayrton.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient clientWebClient() {
        return WebClient.builder()
                .baseUrl("http://localhost:8081") // URL del servicio cliente
                .build();
    }
}