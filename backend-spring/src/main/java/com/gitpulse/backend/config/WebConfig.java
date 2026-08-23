package com.gitpulse.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Global Web Configuration for GitPulse.
 *
 * Why CORS configuration:
 * The frontend is built with React (typically running on localhost:5173 for Vite or localhost:3000 for CRA).
 * We enable CORS across all endpoints so the frontend can query REST APIs and listen to SSE streams.
 *
 * Why RestClient Bean:
 * RestClient is Spring Boot 3's modern, fluent synchronous HTTP client, offering high performance
 * and cleaner syntax than legacy RestTemplate.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("User-Agent", "GitPulse-Enterprise-App/1.0")
                .build();
    }
}
