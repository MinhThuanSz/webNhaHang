package com.example.restaurantpro.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.restaurantpro.model.Booking;
import com.example.restaurantpro.model.DiningTable;
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
        return "booking/start";
    }

    @PostMapping("/booking/tables")
    public String availableTables(@RequestParam Integer guestCount,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime bookingDateTime,
                                  Model model) {
        List<DiningTable> suitableTables = tableService.findSuitableTables(guestCount);
        model.addAttribute("tables", suitableTables);
        model.addAttribute("guestCount", guestCount);
        model.addAttribute("bookingDateTime", bookingDateTime);
        return "booking/tables";
    }

    @PostMapping("/booking/preorder")
    public String preOrder(@RequestParam Long tableId,
                           @RequestParam Integer guestCount,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime bookingDateTime,
                           Model model) {
        model.addAttribute("selectedTable", tableService.getTableById(tableId));
        model.addAttribute("guestCount", guestCount);
        model.addAttribute("bookingDateTime", bookingDateTime);
        model.addAttribute("groupedMenu", menuService.getGroupedAvailableMenu());
        model.addAttribute("estimatedBase", BigDecimal.ZERO);
        model.addAttribute("vnpayMinimum", BigDecimal.valueOf(5000));
        return "booking/preorder";
    }

    @PostMapping("/booking/confirm")
    public String confirmBooking(@RequestParam Long tableId,
                                 @RequestParam Integer guestCount,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime bookingDateTime,
                                 @RequestParam(required = false) String notes,
                                 @RequestParam(defaultValue = "PAY_AT_RESTAURANT") PaymentMethod paymentMethod,
                                 Authentication authentication,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        Map<Long, Integer> selectedItems = extractSelectedItems(request);

        Booking booking = bookingService.createBooking(
                authentication.getName(),
                tableId,
                guestCount,
                bookingDateTime,
                notes,
                selectedItems,
                paymentMethod
        );

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
                                     @RequestParam(required = false) String notes,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        bookingService.createBooking(authentication.getName(), tableId, guestCount, bookingDateTime, notes, Map.of(), PaymentMethod.PAY_AT_RESTAURANT);
        redirectAttributes.addFlashAttribute("successMessage", "Dat ban thanh cong. Ban chua chon mon truoc.");
        return "redirect:/booking/my";
    }

    @GetMapping("/booking/my")
    public String myBookings(Authentication authentication, Model model) {
        model.addAttribute("bookings", bookingService.getBookingsForUser(authentication.getName()));
        return "booking/my-bookings";
    }

    private Map<Long, Integer> extractSelectedItems(HttpServletRequest request) {
        Map<Long, Integer> selectedItems = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (key.startsWith("qty_")) {
                try {
                    Long itemId = Long.parseLong(key.replace("qty_", ""));
                    Integer quantity = Integer.parseInt(values[0]);
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
