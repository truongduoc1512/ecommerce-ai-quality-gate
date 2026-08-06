package com.example.demo.config;

import org.springdoc.core.GroupedOpenApi;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ShoeShop API & Controller Documentation")
                        .version("1.0.0")
                        .description("Tài liệu API đầy đủ bao gồm tất cả các Controllers (Main, Admin, Auth) cho ShoeShop.")
                        .contact(new Contact().name("ShoeShop Development Team")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("all-endpoints")
                .pathsToMatch("/**")
                .packagesToScan("com.example.demo.controller")
                .build();
    }
}
