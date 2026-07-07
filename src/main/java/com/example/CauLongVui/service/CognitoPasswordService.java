package com.example.CauLongVui.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CodeDeliveryDetailsType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmForgotPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordResponse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class CognitoPasswordService {

    private final CognitoIdentityProviderClient cognitoClient;

    @Value("${spring.security.oauth2.client.registration.cognito.client-id:}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.cognito.client-secret:}")
    private String clientSecret;

    public String sendForgotPasswordCode(String email) {
        ensureConfigured();
        String username = normalizeEmail(email);

        try {
            ForgotPasswordRequest.Builder builder = ForgotPasswordRequest.builder()
                    .clientId(clientId)
                    .username(username);
            applySecretHash(builder, username);

            ForgotPasswordResponse response = cognitoClient.forgotPassword(builder.build());

            CodeDeliveryDetailsType details = response.codeDeliveryDetails();
            if (details == null) {
                return "Neu email ton tai trong Cognito, ma xac nhan se duoc gui den email cua ban.";
            }

            String destination = details.destination() != null ? details.destination() : "email cua ban";
            return "Ma xac nhan da duoc gui den " + destination + ".";
        } catch (CognitoIdentityProviderException ex) {
            throw new IllegalArgumentException(toVietnameseMessage(ex));
        }
    }

    public void confirmForgotPassword(String email, String confirmationCode, String newPassword) {
        ensureConfigured();
        String username = normalizeEmail(email);

        try {
            ConfirmForgotPasswordRequest.Builder builder = ConfirmForgotPasswordRequest.builder()
                    .clientId(clientId)
                    .username(username)
                    .confirmationCode(confirmationCode.trim())
                    .password(newPassword);
            applySecretHash(builder, username);

            cognitoClient.confirmForgotPassword(builder.build());
        } catch (CognitoIdentityProviderException ex) {
            throw new IllegalArgumentException(toVietnameseMessage(ex));
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private void ensureConfigured() {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("Chua cau hinh Cognito client-id");
        }
    }

    private String secretHash(String username) {
        if (clientSecret == null || clientSecret.isBlank()) {
            return null;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(key);
            byte[] raw = mac.doFinal((username + clientId).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception ex) {
            throw new IllegalStateException("Khong the tao Cognito secret hash", ex);
        }
    }

    private void applySecretHash(ForgotPasswordRequest.Builder builder, String username) {
        String hash = secretHash(username);
        if (hash != null) {
            builder.secretHash(hash);
        }
    }

    private void applySecretHash(ConfirmForgotPasswordRequest.Builder builder, String username) {
        String hash = secretHash(username);
        if (hash != null) {
            builder.secretHash(hash);
        }
    }

    private String toVietnameseMessage(CognitoIdentityProviderException ex) {
        String code = ex.awsErrorDetails() != null ? ex.awsErrorDetails().errorCode() : "";
        return switch (code) {
            case "UserNotFoundException" -> "Email nay chua ton tai trong Cognito.";
            case "InvalidParameterException" -> "Thong tin khong hop le hoac tai khoan chua duoc xac minh.";
            case "LimitExceededException" -> "Ban da yeu cau qua nhieu lan. Vui long thu lai sau.";
            case "CodeMismatchException" -> "Ma xac nhan khong dung.";
            case "ExpiredCodeException" -> "Ma xac nhan da het han. Vui long yeu cau ma moi.";
            case "InvalidPasswordException" -> "Mat khau moi khong dat chinh sach bao mat cua Cognito.";
            case "NotAuthorizedException" -> "Cognito khong cho phep thao tac nay voi App Client hien tai.";
            default -> "Loi Cognito: " + ex.awsErrorDetails().errorMessage();
        };
    }
}
