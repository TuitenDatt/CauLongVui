package com.example.CauLongVui.config;

import com.example.CauLongVui.dto.AuthResponse;
import com.example.CauLongVui.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Xử lý sau khi đăng nhập OAuth2 (Google qua Cognito) thành công.
 *
 * Redirect về frontend (CloudFront) thay vì relative path,
 * vì frontend và backend nằm trên các domain khác nhau.
 */
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;

    /**
     * URL frontend (CloudFront domain hoặc localhost khi dev).
     * Được inject từ app.frontend-url trong application.properties.
     */
    @Value("${app.frontend-url:http://localhost:8080}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        AuthResponse authResponse = authService.loginWithOAuth2(oauthUser);

        // Redirect về CloudFront URL (absolute), không dùng relative path
        // vì sau khi tách frontend, /auth/oauth2-callback.html nằm trên S3/CloudFront
        String callbackUrl = UriComponentsBuilder
                .fromUriString(frontendUrl)
                .path("/auth/oauth2-callback.html")
                .queryParam("userId", authResponse.getId())
                .build()
                .toUriString();

        response.sendRedirect(callbackUrl);
    }
}
