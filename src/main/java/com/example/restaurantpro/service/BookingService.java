package com.example.restaurantpro.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.restaurantpro.dto.DailyRevenueDto;
import com.example.restaurantpro.model.AppUser;
import com.example.restaurantpro.model.Booking;
import com.example.restaurantpro.model.BookingItem;
import com.example.restaurantpro.model.BookingStatus;
import com.example.restaurantpro.model.DiningTable;
import com.example.restaurantpro.model.MenuItem;
import com.example.restaurantpro.model.PaymentMethod;
import com.example.restaurantpro.model.PaymentStatus;
import com.example.restaurantpro.model.PaymentTransaction;
import com.example.restaurantpro.model.PaymentTransactionType;
import com.example.restaurantpro.repository.BookingRepository;
import com.example.restaurantpro.repository.PaymentTransactionRepository;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final AppUserService appUserService;
    private final TableService tableService;
    private final MenuService menuService;
    private final PaymentTransactionRepository paymentTransactionRepository;

    public BookingService(BookingRepository bookingRepository,
                          AppUserService appUserService,
                          TableService tableService,
                          MenuService menuService,
                          PaymentTransactionRepository paymentTransactionRepository) {
        this.bookingRepository = bookingRepository;
        this.appUserService = appUserService;
        this.tableService = tableService;
        this.menuService = menuService;
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    public BigDecimal getRevenueByDate(LocalDate date) {
        return bookingRepository.getRevenueByDate(date, PaymentStatus.PAID);
    }

    public BigDecimal getRevenueByMonth(Integer month, Integer year) {
        return bookingRepository.getRevenueByMonth(month, year, PaymentStatus.PAID);
    }

    public Long countPaidBookingsByDate(LocalDate date) {
        return bookingRepository.countPaidBookingsByDate(date, PaymentStatus.PAID);
    }

    public Long countPaidBookingsByMonth(Integer month, Integer year) {
        return bookingRepository.countPaidBookingsByMonth(month, year, PaymentStatus.PAID);
    }

    public List<DailyRevenueDto> getRevenueStatsByDaysInMonth(Integer month, Integer year) {
        return bookingRepository.getRevenueStatsByDaysInMonth(month, year, PaymentStatus.PAID);
    }

    public Booking createBooking(String customerPhone,
                                 Long tableId,
                                 Integer guestCount,
                                 LocalDateTime bookingDateTime,
                                 String notes,
                                 Map<Long, Integer> selectedItems,
                                 PaymentMethod paymentMethod) {

        AppUser customer = appUserService.findByPhone(customerPhone)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay khach hang."));
        DiningTable table = tableService.getTableById(tableId);

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setDiningTable(table);
        booking.setGuestCount(guestCount);
        booking.setBookingDateTime(bookingDateTime);
        booking.setNotes(notes);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentMethod(paymentMethod == null ? PaymentMethod.PAY_AT_RESTAURANT : paymentMethod);
        booking.setPaymentStatus(PaymentStatus.UNPAID);

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : selectedItems.entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }

            MenuItem menuItem = menuService.findById(entry.getKey());
            BookingItem bookingItem = new BookingItem(menuItem, entry.getValue(), menuItem.getPrice());
            booking.addItem(bookingItem);
            total = total.add(menuItem.getPrice().multiply(BigDecimal.valueOf(entry.getValue())));
        }

        booking.setTotalAmount(total);
        return bookingRepository.save(booking);
    }

    public Booking save(Booking booking) {
        return bookingRepository.save(booking);
    }

    public List<Booking> getBookingsForUser(String phone) {
        return bookingRepository.findByCustomer_PhoneOrderByBookingDateTimeDesc(phone);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByBookingDateTimeDesc();
    }

    public List<Booking> getBookingsByDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return bookingRepository.findByBookingDateTimeBetweenOrderByBookingDateTimeAsc(start, end);
    }

    public Booking findById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay don dat ban."));
    }

    public void cancel(Long id) {
        Booking booking = findById(id);
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    public void markNoShow(Long id) {
        Booking booking = findById(id);
        booking.setStatus(BookingStatus.NO_SHOW);
        bookingRepository.save(booking);
    }

    public void markPaymentPending(Booking booking, String txnRef) {
        booking.setPaymentMethod(PaymentMethod.VNPAY);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setLatestPaymentTxnRef(txnRef);
        bookingRepository.save(booking);
    }

    public void markPaid(Booking booking, String txnRef, LocalDateTime paidAt) {
        booking.setPaymentMethod(PaymentMethod.VNPAY);
        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setLatestPaymentTxnRef(txnRef);
        booking.setPaidAt(paidAt == null ? LocalDateTime.now() : paidAt);
        bookingRepository.save(booking);
    }

    public void markPaymentFailed(Booking booking, String txnRef) {
        booking.setPaymentMethod(PaymentMethod.VNPAY);
        booking.setPaymentStatus(PaymentStatus.FAILED);
        booking.setLatestPaymentTxnRef(txnRef);
        bookingRepository.save(booking);
    }

    public void markRefundPending(Booking booking) {
        booking.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        bookingRepository.save(booking);
    }

    public void markRefunded(Booking booking, LocalDateTime refundedAt) {
        booking.setPaymentStatus(PaymentStatus.REFUNDED);
        bookingRepository.save(booking);
    }

    public void manuallyConfirmPaid(Long bookingId, String operatorName) {
        Booking booking = findById(bookingId);
        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            return;
        }
        if (booking.getPaymentStatus() == PaymentStatus.REFUNDED
                || booking.getPaymentStatus() == PaymentStatus.REFUND_PENDING) {
            throw new IllegalStateException("Don nay dang o trang thai hoan tien, khong the xac nhan da thanh toan.");
        }

        booking.setPaymentStatus(PaymentStatus.PAID);
        if (booking.getPaidAt() == null) {
            booking.setPaidAt(LocalDateTime.now());
        }

        PaymentTransaction manualTransaction = new PaymentTransaction(
                booking,
                "MANUAL-" + booking.getId() + "-" + System.currentTimeMillis(),
                booking.getTotalAmount() == null ? BigDecimal.ZERO : booking.getTotalAmount()
        );
        manualTransaction.setProvider("MANUAL");
        manualTransaction.setType(PaymentTransactionType.PAYMENT);
        manualTransaction.setStatus(PaymentStatus.PAID);
        manualTransaction.setCreatedBy(operatorName);
        manualTransaction.setMessage("Admin xac nhan da thanh toan cho booking #" + booking.getId());
        manualTransaction.setPaidAt(booking.getPaidAt());
        booking.addPaymentTransaction(manualTransaction);

        bookingRepository.save(booking);
    }

    public long countBookings() {
        return bookingRepository.count();
    }

    public long countPreOrderedItems() {
        return bookingRepository.findAll().stream()
                .flatMap(booking -> booking.getItems().stream())
                .mapToLong(item -> item.getQuantity())
                .sum();
    }

    public List<Booking> getUpcomingBookings(int limit) {
        LocalDateTime now = LocalDateTime.now();
        return bookingRepository.findAll().stream()
                .filter(booking -> booking.getBookingDateTime() != null && booking.getBookingDateTime().isAfter(now))
                .sorted((a, b) -> a.getBookingDateTime().compareTo(b.getBookingDateTime()))
                .limit(limit)
                .toList();
    }
}