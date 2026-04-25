package com.tynysai.appointmentservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI appointmentServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TynysAI Appointment Service API")
                        .description("Запись на приём к врачу: статусы PENDING/ACCEPTED/REJECTED/COMPLETED")
                        .version("1.0.0")
                        .contact(new Contact().name("TynysAI").email("basylbekov@crystalspring.kz"))
                        .license(new License().name("Apache 2.0")));
    }
}