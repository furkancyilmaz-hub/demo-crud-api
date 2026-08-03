package com.furkan.democrudapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadata shown at the top of the Swagger page. Deliberately no {@code GroupedOpenApi} bean:
 * any grouping with a path filter would silently drop the {@code /internal/**} controllers,
 * and the requirement is that every endpoint without exception is reachable from Swagger.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI demoCrudApiOpenApi() {
        return new OpenAPI().info(new Info()
                .title("demo-crud-api")
                .version("v1")
                .description("Customer, proposal and payment CRUD endpoints, plus the internal "
                        + "log, request and schema endpoints used by the N+1 analysis."));
    }
}
