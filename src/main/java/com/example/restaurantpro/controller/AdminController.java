package com.example.restaurantpro.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.restaurantpro.model.Booking;
import com.example.restaurantpro.model.DiningTable;
import com.example.restaurantpro.model.MenuCategory;
import com.example.restaurantpro.model.MenuItem;
import com.example.restaurantpro.model.RoleName;
import com.example.restaurantpro.service.AppUserService;
import com.example.restaurantpro.service.BookingService;
import com.example.restaurantpro.service.MenuService;
import com.example.restaurantpro.service.TableService;
import com.example.restaurantpro.service.VnPayService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final TableService tableService;
    private final MenuService menuService;
    private final AppUserService appUserService;
    private final BookingService bookingService;
    private final VnPayService vnPayService;

    public AdminController(TableService tableService,
                           MenuService menuService,
                           AppUserService appUserService,
                           BookingService bookingService,
                           VnPayService vnPayService) {
        this.tableService = tableService;
        this.menuService = menuService;
        this.appUserService = appUserService;
        this.bookingService = bookingService;
        this.vnPayService = vnPayService;
    }

    @GetMapping
    public String adminHome() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("customerCount", appUserService.countCustomers());
        model.addAttribute("tableCount", tableService.countTables());
        model.addAttribute("bookingCount", bookingService.countBookings());
        model.addAttribute("preOrderCount", bookingService.countPreOrderedItems());
        model.addAttribute("upcomingBookings", bookingService.getUpcomingBookings(6));
        return "admin/dashboard";
    }

    @GetMapping("/revenue")
    public String revenue(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Model model) {

        LocalDate now = LocalDate.now();
        LocalDate selectedDate = (date != null) ? date : now;
        int selectedMonth = (month != null) ? month : now.getMonthValue();
        int selectedYear = (year != null) ? year : now.getYear();

        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("selectedYear", selectedYear);

        model.addAttribute("dailyRevenue", bookingService.getRevenueByDate(selectedDate));
        model.addAttribute("monthlyRevenue", bookingService.getRevenueByMonth(selectedMonth, selectedYear));
        model.addAttribute("dailyPaidBookings", bookingService.countPaidBookingsByDate(selectedDate));
        model.addAttribute("monthlyPaidBookings", bookingService.countPaidBookingsByMonth(selectedMonth, selectedYear));
        model.addAttribute("revenueByDaysInMonth", bookingService.getRevenueStatsByDaysInMonth(selectedMonth, selectedYear));

        return "admin/revenue";
    }

    @GetMapping("/tables")
public String tables(@RequestParam(required = false) Long editId, Model model) {
    model.addAttribute("tables", tableService.getAllTables());

    if (editId != null) {
        model.addAttribute("tableForm", tableService.getTableById(editId));
    } else {
        model.addAttribute("tableForm", new DiningTable());
    }

    return "admin/tables";
}

    @PostMapping("/tables/save")
    public String saveTable(@RequestParam(required = false) Long id,
                            @RequestParam String name,
                            @RequestParam String location,
                            @RequestParam Integer capacity,
                            @RequestParam String style,
                            @RequestParam String chairType,
                            @RequestParam String description,
                            RedirectAttributes redirectAttributes) {
        tableService.saveOrUpdate(id, name, location, capacity, style, chairType, description);
        redirectAttributes.addFlashAttribute("successMessage", "Đã lưu thông tin bàn ăn.");
        return "redirect:/admin/tables";
    }

    @PostMapping("/tables/delete")
    public String deleteTable(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        tableService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xoá bàn ăn.");
        return "redirect:/admin/tables";
    }

    @GetMapping("/menu")
public String menu(@RequestParam(required = false) Long editId, Model model) {
    model.addAttribute("menuItems", menuService.findAllForAdmin());
    model.addAttribute("categories", List.of(MenuCategory.values()));

    if (editId != null) {
        model.addAttribute("menuItemForm", menuService.findById(editId));
    } else {
        model.addAttribute("menuItemForm", new MenuItem());
    }

    return "admin/menu";
}

    @PostMapping("/menu/save")
    public String saveMenu(@RequestParam(required = false) Long id,
                           @RequestParam String name,
                           @RequestParam MenuCategory category,
                           @RequestParam String description,
                           @RequestParam BigDecimal price,
                           @RequestParam String imageUrl,
                           RedirectAttributes redirectAttributes) {
        menuService.saveOrUpdate(id, name, category, description, price, imageUrl);
        redirectAttributes.addFlashAttribute("successMessage", "Đã lưu món ăn.");
        return "redirect:/admin/menu";
    }

    @PostMapping("/menu/delete")
    public String deleteMenu(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        menuService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa món ăn.");
        return "redirect:/admin/menu";
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", appUserService.findAllUsers());
        model.addAttribute("roles", Arrays.asList(RoleName.values()));
        return "admin/users";
    }

    @PostMapping("/users/grant")
    public String grantRole(@RequestParam Long userId,
                            @RequestParam RoleName roleName,
                            RedirectAttributes redirectAttributes) {
        appUserService.grantRole(userId, roleName);
        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật quyền cho tài khoản.");
        return "redirect:/admin/users";
    }

    @GetMapping("/bookings")
    public String bookings(@RequestParam(required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bookingDate,
                           Model model) {
        model.addAttribute("selectedDate", bookingDate);
        model.addAttribute("bookings", bookingDate == null
                ? bookingService.getAllBookings()
                : bookingService.getBookingsByDate(bookingDate));
        return "admin/bookings";
    }

    @PostMapping("/bookings/cancel")
    public String cancelBooking(@RequestParam Long bookingId, RedirectAttributes redirectAttributes) {
        bookingService.cancel(bookingId);
        redirectAttributes.addFlashAttribute("successMessage", "Đơn đặt bàn đã được hủy.");
        return "redirect:/admin/bookings";
    }

    @PostMapping("/bookings/no-show")
    public String noShowBooking(@RequestParam Long bookingId, RedirectAttributes redirectAttributes) {
        bookingService.markNoShow(bookingId);
        redirectAttributes.addFlashAttribute("successMessage", "Đã đánh dấu khách không đến.");
        return "redirect:/admin/bookings";
    }

    @PostMapping("/bookings/confirm-paid")
    public String confirmPaid(@RequestParam Long bookingId,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        String operatorName = authentication != null ? authentication.getName() : "admin";
        bookingService.manuallyConfirmPaid(bookingId, operatorName);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xác nhận đơn đã thanh toán.");
        return "redirect:/admin/bookings";
    }

    @PostMapping("/bookings/refund")
    public String refundBooking(@RequestParam Long bookingId,
                                Authentication authentication,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        Booking booking = bookingService.findById(bookingId);
        String operatorName = authentication != null ? authentication.getName() : "admin";
        String message = vnPayService.refundBooking(booking, operatorName, request);
        redirectAttributes.addFlashAttribute("successMessage", message);
        return "redirect:/admin/bookings";
    }
}