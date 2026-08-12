package com.learnpulse.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LearnPulse AI - Learning Assistant Backend API")
                        .description("Backend Infrastructure & Foundation APIs for LearnPulse AI Learning Management System with Contextual AI Tutor")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("LearnPulse Engineering Team")
                                .email("engineering@learnpulse.ai"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://learnpulse.ai")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Standard Authorization header using Bearer scheme. Example: \"Bearer {token}\"")));
    }
}
