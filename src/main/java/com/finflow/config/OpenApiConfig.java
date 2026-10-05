package com.finflow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI finFlowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FinFlow Digital Banking API")
                        .version("1.0.0")
                        .description("Backend API for user registration, accounts, fund transfers "
                                + "and transaction history. Protected endpoints need a JWT token "
                                + "(login first, then click Authorize).")
                        .contact(new Contact()
                                .name("FinFlow Developer")
                                .email("your-email@example.com")))
                // Har endpoint ke liye default: JWT chahiye
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}