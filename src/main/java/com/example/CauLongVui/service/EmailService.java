package com.example.CauLongVui.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final SesV2Client sesV2Client;

    @Value("${app.mail.enabled:false}")
    private boolean enabled;

    @Value("${app.mail.from:no-reply@example.com}")
    private String fromAddress;

    public boolean isEnabled() {
        return enabled;
    }

    public boolean sendHtmlEmail(String to, String subject, String textBody, String htmlBody) {
        if (!enabled) {
            log.info("Email disabled. Would send '{}' to {}", subject, to);
            return false;
        }
        if (to == null || to.isBlank()) {
            log.warn("Skipped email '{}' because recipient is blank", subject);
            return false;
        }

        SendEmailRequest request = SendEmailRequest.builder()
                .fromEmailAddress(fromAddress)
                .destination(Destination.builder().toAddresses(to).build())
                .content(EmailContent.builder()
                        .simple(Message.builder()
                                .subject(Content.builder()
                                        .charset("UTF-8")
                                        .data(subject)
                                        .build())
                                .body(Body.builder()
                                        .text(Content.builder()
                                                .charset("UTF-8")
                                                .data(textBody)
                                                .build())
                                        .html(Content.builder()
                                                .charset("UTF-8")
                                                .data(htmlBody)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        try {
            sesV2Client.sendEmail(request);
            log.info("Sent email '{}' to {}", subject, to);
            return true;
        } catch (Exception ex) {
            log.error("Failed to send email '{}' to {}: {}", subject, to, ex.getMessage(), ex);
            return false;
        }
    }
}
