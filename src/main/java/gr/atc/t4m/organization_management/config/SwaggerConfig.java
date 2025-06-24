package gr.atc.t4m.organization_management.config;

import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
@Profile("!test")
public class SwaggerConfig {
    @Value(value = "${build.version}")
    private String appVersion;

    @Value(value = "${application.url}")
    private String appUrl;

    @Bean
    public OpenAPI openAPIDocumentation() {
        return new OpenAPI()
                .info(new Info()
                        .title("Organization API")
                        .version(appVersion)
                        .description("API documentation for Organization service"))
                .openapi("3.0.3")
                .addServersItem(new Server().url(appUrl))
                .components(new Components()
                  .addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}