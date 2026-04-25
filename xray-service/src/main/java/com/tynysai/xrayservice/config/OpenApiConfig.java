package com.tynysai.xrayservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI xrayServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TynysAI Xray Service API")
                        .description("Загрузка рентген-снимков, AI-анализ через Python FastAPI и валидация врачом")
                        .version("1.0.0")
                        .contact(new Contact().name("TynysAI").email("basylbekov@crystalspring.kz"))
                        .license(new License().name("Apache 2.0")));
    }
}