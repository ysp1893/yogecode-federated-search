package com.yogecode.federatedsearch.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI federatedSearchOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Yogecode Federated Search API")
                        .description("Metadata-driven admin and search APIs for federated data access across SQL and NoSQL sources.")
                        .version("v0.0.1")
                        .contact(new Contact()
                                .name("Yogecode")
                                .url("https://yogecode.com"))
                        .license(new License()
                                .name("Internal Starter Project")));
    }
}
