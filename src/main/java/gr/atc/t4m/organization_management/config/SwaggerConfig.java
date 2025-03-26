package gr.atc.t4m.organization_management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPIDocumentation() {
        return new OpenAPI()
                .info(new Info().title("Organization Manager API").version("0.5").description("API documentation for Organization Manager service"))
                .components(new Components()
                );
    }
}
