package com.example.CauLongVui.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository,
            ObjectProvider<CognitoAuthorizationRequestResolver> cognitoAuthorizationRequestResolver,
            OAuth2LoginSuccessHandler oauth2LoginSuccessHandler,
            CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // Bật CORS với config từ WebConfig.corsConfigurationSource()
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll());

        if (clientRegistrationRepository.getIfAvailable() != null) {
            http.oauth2Login(oauth2 -> oauth2
                    .loginPage("/auth/login.html")
                    .authorizationEndpoint(authorization -> cognitoAuthorizationRequestResolver
                            .ifAvailable(authorization::authorizationRequestResolver))
                    .successHandler(oauth2LoginSuccessHandler)
                    .failureUrl("/auth/login.html?oauthError=true"));
        }

        return http.build();
    }
}
