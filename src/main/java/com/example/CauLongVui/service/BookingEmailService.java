package com.example.CauLongVui.service;

import com.example.CauLongVui.entity.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingEmailService {

    private static final Locale VIETNAM = Locale.forLanguageTag("vi-VN");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", VIETNAM);
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    private final EmailService emailService;

    public boolean isEnabled() {
        return emailService.isEnabled();
    }

    public boolean sendBookingCreatedEmail(Booking booking) {
        String email = getRecipientEmail(booking);
        if (email == null) {
            log.info("Skipped booking confirmation email for booking #{} because user email is missing",
                    booking.getId());
            return false;
        }

        String subject = "Xác nhận đặt sân thành công - Cầu Lông Vui";
        return emailService.sendHtmlEmail(
                email,
                subject,
                buildTextBody("Đặt sân thành công", booking),
                buildHtmlBody("Đặt sân thành công", introForCreated(booking), booking));
    }

    public boolean sendBookingReminderEmail(Booking booking) {
        String email = getRecipientEmail(booking);
        if (email == null) {
            log.info("Skipped booking reminder email for booking #{} because user email is missing",
                    booking.getId());
            return false;
        }

        String subject = "Nhắc lịch đặt sân trong 12 tiếng tới - Cầu Lông Vui";
        return emailService.sendHtmlEmail(
                email,
                subject,
                buildTextBody("Nhắc lịch đặt sân", booking),
                buildHtmlBody("Nhắc lịch đặt sân",
                        "Bạn có lịch chơi cầu lông sắp tới. Vui lòng đến sớm 10-15 phút để nhận sân.",
                        booking));
    }

    private String getRecipientEmail(Booking booking) {
        if (booking.getUser() == null || booking.getUser().getEmail() == null
                || booking.getUser().getEmail().isBlank()) {
            return null;
        }
        return booking.getUser().getEmail();
    }

    private String introForCreated(Booking booking) {
        if (booking.getStatus() == Booking.BookingStatus.PENDING) {
            return "Cảm ơn bạn đã đặt sân. Booking của bạn đã được ghi nhận và đang chờ thanh toán/xác nhận.";
        }
        return "Cảm ơn bạn đã đặt sân. Booking của bạn đã được xác nhận thành công.";
    }

    private String buildTextBody(String title, Booking booking) {
        return """
                %s

                Xin chào %s,

                Thông tin đặt sân:
                - Mã booking: #%d
                - Sân: %s
                - Ngày: %s
                - Thời gian: %s - %s
                - Trạng thái: %s
                - Tổng tiền: %s

                Cảm ơn bạn đã sử dụng Cầu Lông Vui.
                """.formatted(
                title,
                safe(booking.getCustomerName()),
                booking.getId(),
                booking.getCourt().getName(),
                booking.getBookingDate().format(DATE_FORMATTER),
                booking.getStartTime().format(TIME_FORMATTER),
                booking.getEndTime().format(TIME_FORMATTER),
                booking.getStatus(),
                formatMoney(booking.getTotalPrice()));
    }

    private String buildHtmlBody(String title, String intro, Booking booking) {
        return """
                <!doctype html>
                <html lang="vi">
                  <head>
                    <meta charset="UTF-8">
                    <title>%s</title>
                  </head>
                  <body style="margin:0;padding:0;background:#f5f7f5;font-family:Arial,'Helvetica Neue',sans-serif;color:#17231a;">
                    <div style="max-width:620px;margin:0 auto;padding:28px 16px;">
                      <div style="background:#ffffff;border:1px solid #e3e8e3;border-radius:10px;overflow:hidden;">
                        <div style="background:#166534;color:#ffffff;padding:22px 26px;">
                          <h1 style="margin:0;font-size:22px;line-height:1.35;">%s</h1>
                          <p style="margin:8px 0 0;font-size:14px;opacity:.9;">Cầu Lông Vui</p>
                        </div>
                        <div style="padding:26px;">
                          <p style="margin:0 0 18px;font-size:15px;line-height:1.6;">Xin chào <strong>%s</strong>,</p>
                          <p style="margin:0 0 22px;font-size:15px;line-height:1.6;">%s</p>
                          <table style="width:100%%;border-collapse:collapse;font-size:14px;">
                            %s
                          </table>
                          <p style="margin:24px 0 0;font-size:13px;line-height:1.6;color:#66736a;">
                            Nếu bạn cần đổi hoặc hủy lịch, vui lòng truy cập Cầu Lông Vui hoặc liên hệ nhân viên hỗ trợ.
                          </p>
                        </div>
                      </div>
                    </div>
                  </body>
                </html>
                """.formatted(
                escapeHtml(title),
                escapeHtml(title),
                escapeHtml(safe(booking.getCustomerName())),
                escapeHtml(intro),
                bookingRows(booking));
    }

    private String bookingRows(Booking booking) {
        return row("Mã booking", "#" + booking.getId())
                + row("Sân", booking.getCourt().getName())
                + row("Ngày", booking.getBookingDate().format(DATE_FORMATTER))
                + row("Thời gian", booking.getStartTime().format(TIME_FORMATTER)
                        + " - " + booking.getEndTime().format(TIME_FORMATTER))
                + row("Trạng thái", booking.getStatus().name())
                + row("Tổng tiền", formatMoney(booking.getTotalPrice()));
    }

    private String row(String label, String value) {
        return """
                <tr>
                  <td style="padding:11px 0;border-bottom:1px solid #edf1ed;color:#66736a;width:35%%;">%s</td>
                  <td style="padding:11px 0;border-bottom:1px solid #edf1ed;font-weight:700;color:#17231a;">%s</td>
                </tr>
                """.formatted(escapeHtml(label), escapeHtml(value));
    }

    private String formatMoney(Double amount) {
        if (amount == null) {
            return "0 ₫";
        }
        return NumberFormat.getCurrencyInstance(VIETNAM).format(amount);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "bạn" : value;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
