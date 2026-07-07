package com.example.CauLongVui.service;

import com.example.CauLongVui.dto.BookingHoldRequest;
import com.example.CauLongVui.dto.BookingHoldResponse;
import com.example.CauLongVui.entity.Booking;
import com.example.CauLongVui.repository.BookingRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BookingHoldServiceTest {

    private final BookingRepository bookingRepository = mock(BookingRepository.class);
    private final BookingHoldService service = new BookingHoldService(bookingRepository);

    @Test
    void holdSlotCreatesActiveHoldWhenSlotIsFree() {
        LocalDate date = LocalDate.now().plusDays(1);
        when(bookingRepository.findByCourtIdAndBookingDate(1L, date)).thenReturn(List.of());

        BookingHoldResponse response = service.holdSlot(request(1L, 10L, date, "08:00", "09:00"));

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getHoldId()).isNotBlank();
        assertThat(service.getHeldSlots(1L, date)).hasSize(1);
    }

    @Test
    void holdSlotRejectsOverlappingConfirmedBooking() {
        LocalDate date = LocalDate.now().plusDays(1);
        Booking booking = Booking.builder()
                .bookingDate(date)
                .startTime(LocalTime.parse("08:30"))
                .endTime(LocalTime.parse("09:30"))
                .status(Booking.BookingStatus.CONFIRMED)
                .build();
        when(bookingRepository.findByCourtIdAndBookingDate(1L, date)).thenReturn(List.of(booking));

        BookingHoldResponse response = service.holdSlot(request(1L, 10L, date, "08:00", "09:00"));

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isNotBlank();
    }

    @Test
    void holdSlotRejectsInvalidTimeRange() {
        LocalDate date = LocalDate.now().plusDays(1);

        assertThatThrownBy(() -> service.holdSlot(request(1L, 10L, date, "09:00", "08:00")))
                .hasMessageContaining("Gio bat dau");
    }

    @Test
    void holdSlotBlocksDifferentUserUntilReleased() {
        LocalDate date = LocalDate.now().plusDays(1);
        when(bookingRepository.findByCourtIdAndBookingDate(1L, date)).thenReturn(List.of());

        BookingHoldResponse first = service.holdSlot(request(1L, 10L, date, "08:00", "09:00"));
        BookingHoldResponse second = service.holdSlot(request(1L, 11L, date, "08:00", "09:00"));

        assertThat(first.isSuccess()).isTrue();
        assertThat(second.isSuccess()).isFalse();

        service.releaseSlot(first.getHoldId());
        BookingHoldResponse afterRelease = service.holdSlot(request(1L, 11L, date, "08:00", "09:00"));
        assertThat(afterRelease.isSuccess()).isTrue();
    }

    @Test
    void holdSlotBlocksOverlappingHoldByDifferentUser() {
        LocalDate date = LocalDate.now().plusDays(1);
        when(bookingRepository.findByCourtIdAndBookingDate(1L, date)).thenReturn(List.of());

        BookingHoldResponse first = service.holdSlot(request(1L, 10L, date, "08:00", "09:00"));
        BookingHoldResponse overlap = service.holdSlot(request(1L, 11L, date, "08:30", "09:30"));

        assertThat(first.isSuccess()).isTrue();
        assertThat(overlap.isSuccess()).isFalse();
    }

    private BookingHoldRequest request(Long courtId, Long userId, LocalDate date, String start, String end) {
        return BookingHoldRequest.builder()
                .courtId(courtId)
                .userId(userId)
                .bookingDate(date)
                .startTime(LocalTime.parse(start))
                .endTime(LocalTime.parse(end))
                .build();
    }
}
