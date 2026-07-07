package com.example.CauLongVui.config;

import com.example.CauLongVui.dto.AuthResponse;
import com.example.CauLongVui.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        AuthResponse authResponse = authService.loginWithOAuth2(oauthUser);

        String callbackUrl = UriComponentsBuilder
                .fromPath("/auth/oauth2-callback.html")
                .queryParam("userId", authResponse.getId())
                .build()
                .toUriString();

        response.sendRedirect(callbackUrl);
    }
}
