package com.example.CauLongVui.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web MVC configuration.
 *
 * Sau khi frontend được host trên S3+CloudFront, Spring Boot chỉ serve /api/**
 * nên không cần addResourceHandlers cho /uploads/** nữa.
 * CORS được cấu hình để cho phép CloudFront domain và localhost gọi vào API.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.frontend-url:http://localhost:8080}")
    private String frontendUrl;

    @Value("${app.cdn-url:http://localhost:8080}")
    private String cdnUrl;

    /**
     * CorsConfigurationSource bean — được dùng bởi SecurityConfig.
     * Cho phép CloudFront domain và localhost gọi API với credentials.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Cho phép frontend URL (CloudFront) và localhost (dev)
        config.setAllowedOrigins(List.of(
                "http://localhost:8080",
                "http://localhost:3000",
                frontendUrl,
                cdnUrl
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        // Cache preflight response trong 1 giờ
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
