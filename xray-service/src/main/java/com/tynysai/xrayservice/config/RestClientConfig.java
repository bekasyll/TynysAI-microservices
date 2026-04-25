package com.tynysai.xrayservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Only the Python AI service is reached via RestClient (multipart file upload).
 * All Java microservice communication uses OpenFeign - see client/UserFeignClient.
 */
@Configuration
public class RestClientConfig {
    @Value("${app.ai.service-url}")
    private String aiServiceUrl;

    @Bean
    public RestClient aiServiceClient() {
        return RestClient.builder().baseUrl(aiServiceUrl).build();
    }
}
