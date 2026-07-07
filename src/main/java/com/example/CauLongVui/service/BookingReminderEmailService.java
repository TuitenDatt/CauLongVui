package com.example.CauLongVui.service;

import com.example.CauLongVui.entity.Booking;
import com.example.CauLongVui.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingReminderEmailService {

    private final BookingRepository bookingRepository;
    private final BookingEmailService bookingEmailService;

    @Value("${app.mail.booking-reminder-hours:12}")
    private long reminderHours;

    @Scheduled(initialDelay = 60 * 1000, fixedDelay = 10 * 60 * 1000)
    @Transactional
    public void sendUpcomingBookingReminders() {
        if (!bookingEmailService.isEnabled()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowEnd = now.plusHours(reminderHours);

        List<Booking> candidates = bookingRepository.findReminderCandidates(
                Booking.BookingStatus.CONFIRMED,
                now.toLocalDate(),
                windowEnd.toLocalDate());

        int sent = 0;
        for (Booking booking : candidates) {
            LocalDateTime startAt = booking.getBookingDate().atTime(booking.getStartTime());
            if (startAt.isBefore(now) || startAt.isAfter(windowEnd)) {
                continue;
            }

            boolean delivered = bookingEmailService.sendBookingReminderEmail(booking);
            if (delivered) {
                booking.setReminderEmailSentAt(LocalDateTime.now());
                sent++;
            }
        }

        if (sent > 0) {
            bookingRepository.saveAll(candidates);
            log.info("Sent {} booking reminder email(s)", sent);
        }
    }
}
