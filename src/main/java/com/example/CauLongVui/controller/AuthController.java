package com.example.CauLongVui.controller;

import com.example.CauLongVui.dto.ApiResponse;
import com.example.CauLongVui.dto.AuthResponse;
import com.example.CauLongVui.dto.ForgotPasswordRequest;
import com.example.CauLongVui.dto.LoginRequest;
import com.example.CauLongVui.dto.RegisterRequest;
import com.example.CauLongVui.dto.ResetPasswordRequest;
import com.example.CauLongVui.dto.UpdateProfileRequest;
import com.example.CauLongVui.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest req) {
        AuthResponse response = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response.getMessage(), response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest req) {
        AuthResponse response = authService.login(req);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> getMe(@RequestParam Long id) {
        AuthResponse response = authService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/oauth2/enabled")
    public ResponseEntity<ApiResponse<Boolean>> isOAuth2Enabled() {
        return ResponseEntity.ok(ApiResponse.success(clientRegistrationRepository.getIfAvailable() != null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest req) {
        String message = authService.forgotPassword(req.getEmail());
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req.getEmail(), req.getConfirmationCode(), req.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Dat lai mat khau thanh cong. Ban co the dang nhap bang mat khau moi.", null));
    }

    @GetMapping("/oauth2/switch-account")
    public void switchOAuth2Account(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.getSession(false);
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }

        ClientRegistrationRepository registrations = clientRegistrationRepository.getIfAvailable();
        if (registrations == null) {
            response.sendRedirect("/auth/login.html?oauthError=true");
            return;
        }

        var registration = registrations.findByRegistrationId("cognito");
        if (registration == null) {
            response.sendRedirect("/auth/login.html?oauthError=true");
            return;
        }

        String restartLoginUrl = appBaseUrl + "/auth/google-switch.html";
        Map<String, Object> metadata = registration.getProviderDetails().getConfigurationMetadata();
        Object endSessionEndpoint = metadata.get("end_session_endpoint");

        if (endSessionEndpoint == null || endSessionEndpoint.toString().isBlank()) {
            response.sendRedirect(restartLoginUrl);
            return;
        }

        String logoutUrl = UriComponentsBuilder
                .fromUriString(endSessionEndpoint.toString())
                .queryParam("client_id", registration.getClientId())
                .queryParam("logout_uri", restartLoginUrl)
                .build()
                .toUriString();

        response.sendRedirect(logoutUrl);
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponse<AuthResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest req) {
        AuthResponse response = authService.updateProfile(req);
        return ResponseEntity.ok(ApiResponse.success("Cap nhat thong tin thanh cong", response));
    }
}
