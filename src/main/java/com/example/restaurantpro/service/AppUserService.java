package com.example.restaurantpro.service;

import com.example.restaurantpro.dto.RegisterRequest;
import com.example.restaurantpro.model.AppUser;
import com.example.restaurantpro.model.RoleName;
import com.example.restaurantpro.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUserService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
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
}
