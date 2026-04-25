package com.tynysai.userservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI userServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TynysAI User Service API")
                        .description("Управление учётными записями, профилями пациентов и врачей, аватарами")
                        .version("1.0.0")
                        .contact(new Contact().name("TynysAI").email("basylbekov@crystalspring.kz"))
                        .license(new License().name("Apache 2.0")));
    }
}