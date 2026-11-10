package com.silvally.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("80억분의 1 대소고 편 API")
                .description("선택형 텍스트 인생 시뮬레이션 REST API")
                .version("v1.1"));
    }
}
