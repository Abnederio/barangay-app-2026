package com.turgo.barangayapp.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient userInfoClient() {
        // You can set the base URL here to keep your Introspector clean
        return WebClient.builder()
                .baseUrl("https://www.googleapis.com")
                .build();
    }
}
