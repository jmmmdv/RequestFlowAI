package com.fromzerotohero.mission.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI missionControlApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Automation Mission Control API")
                        .version("v1")
                        .description("Tenant-aware work management and auditable agent planning API.")
                        .contact(new Contact().name("Jeyhun Mammadov"))
                        .license(new License().name("Portfolio project")))
                .components(new Components().addSecuritySchemes("bearer-jwt",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("OIDC access token with sub, tenant_id, and roles claims")));
    }
}
