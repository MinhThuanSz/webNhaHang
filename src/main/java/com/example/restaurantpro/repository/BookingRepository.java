package com.example.restaurantpro.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.restaurantpro.dto.DailyRevenueDto;
import com.example.restaurantpro.model.Booking;
import com.example.restaurantpro.model.PaymentStatus;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByCustomer_PhoneOrderByBookingDateTimeDesc(String phone);

    List<Booking> findAllByOrderByBookingDateTimeDesc();

    List<Booking> findByBookingDateTimeBetweenOrderByBookingDateTimeAsc(LocalDateTime start, LocalDateTime end);

    @Query("""
        select coalesce(sum(b.totalAmount), 0)
        from Booking b
        where b.paymentStatus = :paidStatus
          and function('date', b.bookingDateTime) = :date
    """)
    BigDecimal getRevenueByDate(@Param("date") LocalDate date,
                                @Param("paidStatus") PaymentStatus paidStatus);

    @Query("""
        select coalesce(sum(b.totalAmount), 0)
        from Booking b
        where b.paymentStatus = :paidStatus
          and month(b.bookingDateTime) = :month
          and year(b.bookingDateTime) = :year
    """)
    BigDecimal getRevenueByMonth(@Param("month") Integer month,
                                 @Param("year") Integer year,
                                 @Param("paidStatus") PaymentStatus paidStatus);

    @Query("""
        select count(b)
        from Booking b
        where b.paymentStatus = :paidStatus
          and function('date', b.bookingDateTime) = :date
    """)
    Long countPaidBookingsByDate(@Param("date") LocalDate date,
                                 @Param("paidStatus") PaymentStatus paidStatus);

    @Query("""
        select count(b)
        from Booking b
        where b.paymentStatus = :paidStatus
          and month(b.bookingDateTime) = :month
          and year(b.bookingDateTime) = :year
    """)
    Long countPaidBookingsByMonth(@Param("month") Integer month,
                                  @Param("year") Integer year,
                                  @Param("paidStatus") PaymentStatus paidStatus);

    @Query("""
        select new com.example.restaurantpro.dto.DailyRevenueDto(
            day(b.bookingDateTime),
            coalesce(sum(b.totalAmount), 0)
        )
        from Booking b
        where b.paymentStatus = :paidStatus
          and month(b.bookingDateTime) = :month
          and year(b.bookingDateTime) = :year
        group by day(b.bookingDateTime)
        order by day(b.bookingDateTime)
    """)
    List<DailyRevenueDto> getRevenueStatsByDaysInMonth(@Param("month") Integer month,
                                                       @Param("year") Integer year,
                                                       @Param("paidStatus") PaymentStatus paidStatus);
}