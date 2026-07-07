package com.example.CauLongVui.repository;

import com.example.CauLongVui.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByCourtId(Long courtId);

    List<Booking> findByUserIdOrderByBookingDateDesc(Long userId);

    List<Booking> findByCustomerPhoneOrderByBookingDateDesc(String phone);

    List<Booking> findByBookingDate(LocalDate date);

    List<Booking> findByCourtIdAndBookingDate(Long courtId, LocalDate date);

    List<Booking> findByIsPassTrueOrderByBookingDateDesc();

    /** Dùng cho scheduler tự động hủy booking đặt trước quá hạn */
    List<Booking> findByPaymentDeadlineBeforeAndPaidAtIsNullAndStatusNot(
            LocalDateTime deadline, Booking.BookingStatus status);

    /** Danh sách booking chưa thanh toán của user */
    List<Booking> findByUserIdAndPaidAtIsNullAndPaymentDeadlineIsNotNullAndStatusNotOrderByBookingDateAsc(
            Long userId, Booking.BookingStatus excludeStatus);

    @Query("""
            select b from Booking b
            join fetch b.court
            left join fetch b.user
            where b.status = :status
              and b.reminderEmailSentAt is null
              and b.user is not null
              and b.bookingDate between :fromDate and :toDate
            """)
    List<Booking> findReminderCandidates(
            Booking.BookingStatus status,
            LocalDate fromDate,
            LocalDate toDate);
}
