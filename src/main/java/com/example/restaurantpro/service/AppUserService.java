package com.example.restaurantpro.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.restaurantpro.dto.RegisterRequest;
import com.example.restaurantpro.model.AppUser;
import com.example.restaurantpro.model.RoleName;
import com.example.restaurantpro.repository.AppUserRepository;
import com.example.restaurantpro.repository.BookingRepository;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUserService(AppUserRepository appUserRepository,
                          BookingRepository bookingRepository,
                          PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.bookingRepository = bookingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AppUser registerCustomer(RegisterRequest request) {
        if (appUserRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new IllegalArgumentException("Số điện thoại này đã được đăng ký.");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
        }

        AppUser user = new AppUser();
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(new LinkedHashSet<>(Set.of(RoleName.ROLE_CUSTOMER)));

        return appUserRepository.save(user);
    }

    public AppUser createSeedUser(String fullName, String phone, String rawPassword, Set<RoleName> roles) {
        Optional<AppUser> existing = appUserRepository.findByPhone(phone);
        if (existing.isPresent()) {
            return existing.get();
        }

        AppUser user = new AppUser();
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRoles(new LinkedHashSet<>(roles));
        return appUserRepository.save(user);
    }

    public Optional<AppUser> findByPhone(String phone) {
        return appUserRepository.findByPhone(phone);
    }

    public List<AppUser> findAllUsers() {
        return appUserRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    public long countCustomers() {
        return appUserRepository.findAll().stream()
                .filter(user -> user.getRoles().contains(RoleName.ROLE_CUSTOMER))
                .count();
    }

    public void grantRole(Long userId, RoleName roleName) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));
        user.getRoles().add(roleName);
        appUserRepository.save(user);
    }

    public boolean toggleRole(Long userId, RoleName roleName) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));

        if (user.getRoles().contains(roleName)) {
            if (user.getRoles().size() <= 1) {
                throw new IllegalArgumentException("Tài khoản phải có ít nhất một quyền.");
            }
            user.getRoles().remove(roleName);
            appUserRepository.save(user);
            return false;
        }

        user.getRoles().add(roleName);
        appUserRepository.save(user);
        return true;
    }

    public void deleteUser(Long userId, String operatorPhone) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));

        if (operatorPhone != null && operatorPhone.equals(user.getPhone())) {
            throw new IllegalArgumentException("Không thể tự xóa tài khoản đang đăng nhập.");
        }

        if (bookingRepository.existsByCustomer_Id(userId)) {
            throw new IllegalArgumentException("Không thể xóa người dùng này vì đã có booking liên quan.");
        }

        appUserRepository.delete(user);
    }
}
