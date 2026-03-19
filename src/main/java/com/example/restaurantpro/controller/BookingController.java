package com.example.restaurantpro.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.restaurantpro.exception.BookingConflictException;
import com.example.restaurantpro.model.Booking;
import com.example.restaurantpro.model.PaymentMethod;
import com.example.restaurantpro.service.BookingService;
import com.example.restaurantpro.service.MenuService;
import com.example.restaurantpro.service.TableService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class BookingController {

    private final TableService tableService;
    private final MenuService menuService;
    private final BookingService bookingService;

    public BookingController(TableService tableService, MenuService menuService, BookingService bookingService) {
        this.tableService = tableService;
        this.menuService = menuService;
        this.bookingService = bookingService;
    }

    @GetMapping("/booking/start")
    public String startBooking(Model model) {
        model.addAttribute("minDateTime", LocalDateTime.now().plusHours(1));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("defaultDurationHours", 2);
        return "booking/start";
    }

    @PostMapping("/booking/tables")
    public String availableTables(@RequestParam Integer guestCount,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime bookingDateTime,
                                  @RequestParam(defaultValue = "2") Integer durationHours,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (bookingDateTime == null || !bookingDateTime.isAfter(LocalDateTime.now())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ban da chon sai ngay gio. Vui long chon thoi gian lon hon hien tai.");
            return "redirect:/booking/start";
        }

        TableService.AvailableTablesResult availableResult = tableService.findAvailableExactCapacityTables(guestCount, bookingDateTime, durationHours);
        model.addAttribute("tables", availableResult.tables());
        model.addAttribute("totalTables", availableResult.totalTables());
        model.addAttribute("availableTables", availableResult.availableTables());
        model.addAttribute("guestCount", guestCount);
        model.addAttribute("bookingDateTime", bookingDateTime);
        model.addAttribute("durationHours", durationHours);
        model.addAttribute("bookingEndTime", bookingDateTime.plusHours(durationHours).plusMinutes(30));
        return "booking/tables";
    }

    @PostMapping("/booking/preorder")
    public String preOrder(@RequestParam Long tableId,
                           @RequestParam Integer guestCount,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime bookingDateTime,
                           @RequestParam(defaultValue = "2") Integer durationHours,
                           Model model) {
        model.addAttribute("selectedTable", tableService.getTableById(tableId));
        model.addAttribute("guestCount", guestCount);
        model.addAttribute("bookingDateTime", bookingDateTime);
        model.addAttribute("durationHours", durationHours);
        model.addAttribute("bookingEndTime", bookingDateTime.plusHours(durationHours).plusMinutes(30));
        model.addAttribute("groupedMenu", menuService.getGroupedAvailableMenu());
        model.addAttribute("estimatedBase", BigDecimal.ZERO);
        model.addAttribute("vnpayMinimum", BigDecimal.valueOf(5000));
        return "booking/preorder";
    }

    @PostMapping("/booking/confirm")
    public String confirmBooking(@RequestParam Long tableId,
                                 @RequestParam Integer guestCount,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime bookingDateTime,
                                 @RequestParam(defaultValue = "2") Integer durationHours,
                                 @RequestParam(required = false) String notes,
                                 @RequestParam(defaultValue = "PAY_AT_RESTAURANT") PaymentMethod paymentMethod,
                                 Authentication authentication,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        Map<Long, Integer> selectedItems = extractSelectedItems(request);

                    Booking booking;
                    try {
                        booking = bookingService.createBooking(
                            authentication.getName(),
                            tableId,
                            guestCount,
                            bookingDateTime,
                            durationHours,
                            notes,
                            selectedItems,
                            paymentMethod
                        );
                    } catch (BookingConflictException ex) {
                        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
                        redirectAttributes.addFlashAttribute("bookingSuggestions", ex.getSuggestions());
                        redirectAttributes.addFlashAttribute("guestCount", guestCount);
                        redirectAttributes.addFlashAttribute("bookingDateTime", bookingDateTime);
                        redirectAttributes.addFlashAttribute("durationHours", durationHours);
                        return "redirect:/booking/start";
                    } catch (IllegalArgumentException | IllegalStateException ex) {
                        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
                        return "redirect:/booking/start";
                    }

        if (paymentMethod == PaymentMethod.VNPAY && booking.hasPayableAmount()) {
            return "redirect:/payment/vnpay/pay?bookingId=" + booking.getId();
        }

        redirectAttributes.addFlashAttribute("successMessage",
                "Dat ban thanh cong cho " + booking.getDiningTable().getName() + ". He thong da ghi nhan don cua ban.");
        return "redirect:/booking/my";
    }

    @PostMapping("/booking/confirm-without-menu")
    public String confirmWithoutMenu(@RequestParam Long tableId,
                                     @RequestParam Integer guestCount,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime bookingDateTime,
                                     @RequestParam(defaultValue = "2") Integer durationHours,
                                     @RequestParam(required = false) String notes,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        try {
            bookingService.createBooking(authentication.getName(), tableId, guestCount, bookingDateTime, durationHours, notes, Map.of(), PaymentMethod.PAY_AT_RESTAURANT);
        } catch (BookingConflictException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("bookingSuggestions", ex.getSuggestions());
            redirectAttributes.addFlashAttribute("guestCount", guestCount);
            redirectAttributes.addFlashAttribute("bookingDateTime", bookingDateTime);
            redirectAttributes.addFlashAttribute("durationHours", durationHours);
            return "redirect:/booking/start";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/booking/start";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Dat ban thanh cong. Ban chua chon mon truoc.");
        return "redirect:/booking/my";
    }

    @GetMapping("/booking/my")
    public String myBookings(Authentication authentication, Model model) {
        model.addAttribute("bookings", bookingService.getBookingsForUser(authentication.getName()));
        return "booking/my-bookings";
    }

    @PostMapping("/booking/my/cancel")
    public String cancelMyBooking(@RequestParam Long bookingId,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        try {
            BookingService.CancelResult result = bookingService.cancelByCustomer(bookingId, authentication.getName());
            if (result.chargedFee()) {
                redirectAttributes.addFlashAttribute("infoMessage",
                        "Ban da huy don. Theo chinh sach, phi huy muon la 30%: " + result.feeAmount().toPlainString() + " VND.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Ban da huy don dat ban thanh cong.");
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/booking/my";
    }

    private Map<Long, Integer> extractSelectedItems(HttpServletRequest request) {
        Map<Long, Integer> selectedItems = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (key.startsWith("qty_")) {
                try {
                    long itemId = Long.parseLong(key.replace("qty_", ""));
                    int quantity = Integer.parseInt(values[0]);
                    if (quantity > 0) {
                        selectedItems.put(itemId, quantity);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        });
        return selectedItems;
    }
}
