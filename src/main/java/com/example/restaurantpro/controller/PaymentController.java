package com.example.restaurantpro.controller;

import com.example.restaurantpro.dto.VnPayCallbackResult;
import com.example.restaurantpro.model.Booking;
import com.example.restaurantpro.service.BookingService;
import com.example.restaurantpro.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class PaymentController {

    private final BookingService bookingService;
    private final VnPayService vnPayService;

    public PaymentController(BookingService bookingService, VnPayService vnPayService) {
        this.bookingService = bookingService;
        this.vnPayService = vnPayService;
    }

    @GetMapping("/payment/vnpay/pay")
    public String redirectToVnPay(@RequestParam Long bookingId,
                                  Authentication authentication,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes) {
        Booking booking = bookingService.findById(bookingId);
        ensureAccess(authentication, booking);

        try {
            String paymentUrl = vnPayService.createPaymentUrl(booking, request);
            return "redirect:" + paymentUrl;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/booking/my";
        }
    }

    @GetMapping("/payment/vnpay/return")
    public String vnpayReturn(HttpServletRequest request, Model model) {
        VnPayCallbackResult result = vnPayService.handleReturn(extractParams(request));
        model.addAttribute("result", result);
        model.addAttribute("responseCode", request.getParameter("vnp_ResponseCode"));
        model.addAttribute("transactionNo", request.getParameter("vnp_TransactionNo"));
        model.addAttribute("bankCode", request.getParameter("vnp_BankCode"));
        return "payment/result";
    }

    @GetMapping("/payment/vnpay/ipn")
    @ResponseBody
    public Map<String, String> vnpayIpn(HttpServletRequest request) {
        return vnPayService.handleIpn(extractParams(request));
    }

    private void ensureAccess(Authentication authentication, Booking booking) {
        if (authentication == null) {
            throw new AccessDeniedException("Ban chua dang nhap.");
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        boolean isOwner = booking.getCustomer() != null && booking.getCustomer().getPhone().equals(authentication.getName());
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Ban khong duoc phep thanh toan booking nay.");
        }
    }

    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> params = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });
        return params;
    }
}
