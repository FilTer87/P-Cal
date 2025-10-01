package com.privatecal.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import io.swagger.v3.oas.models.security.SecurityScheme.In;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI configuration for P-Cal API documentation
 */
@Configuration
public class OpenApiConfig {
    
    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("P-Cal API")
                .description("REST API for P-Cal - Private and secure Calendar Application with activity management and Notifications")
                .version("v0.9.1-beta")
                .contact(new Contact()
                    .name("Filippo Terenzi")
                    .url("https://github.com/FilTer87")
                )
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .addSecurityItem(new SecurityRequirement()
                .addList(SECURITY_SCHEME_NAME))
            .components(new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                    .name(SECURITY_SCHEME_NAME)
                    .type(Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .in(In.HEADER)
                    .description("JWT token obtained from /api/auth/login endpoint")));
    }
}