package com.silvally.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 개발 서버는 Vite 프록시로 같은 출처를 쓰므로 기본값은 CORS를 열지 않는다.
 * 프론트를 별도 도메인에 배포할 때만 CORS_ALLOWED_ORIGINS에 출처를 지정한다.
 */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig implements WebMvcConfigurer {

    private final CorsProperties properties;

    public CorsConfig(CorsProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = properties.allowedOrigins().stream()
                .map(String::trim).filter(origin -> !origin.isEmpty()).toArray(String[]::new);
        if (origins.length == 0) {
            return;
        }
        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Idempotency-Key", "X-Expected-Version")
                .exposedHeaders("Location")
                .maxAge(3600);
    }
}
