package com.example.restaurantpro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.example.restaurantpro.service.CustomUserDetailsService;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService,
                          CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler,
                          CustomAccessDeniedHandler customAccessDeniedHandler) {
        this.customUserDetailsService = customUserDetailsService;
        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authenticationProvider(authenticationProvider());

        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/register", "/403", "/css/**", "/js/**", "/images/**", "/payment/vnpay/return", "/payment/vnpay/ipn").permitAll()
                        .requestMatchers("/admin/users/**").hasRole("ADMIN")
                        .requestMatchers("/admin/bookings/**").hasAnyRole("ADMIN", "TABLE_MANAGER")
                        .requestMatchers("/admin/tables/**").hasAnyRole("ADMIN", "TABLE_MANAGER")
                        .requestMatchers("/api/admin/tables/**").hasAnyRole("ADMIN", "TABLE_MANAGER")
                        .requestMatchers("/admin/kitchen-orders/**").hasAnyRole("ADMIN", "MENU_MANAGER")
                        .requestMatchers("/admin/menu/**").hasAnyRole("ADMIN", "MENU_MANAGER")
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "TABLE_MANAGER", "MENU_MANAGER")
                        .requestMatchers("/booking/**", "/payment/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(customAuthenticationSuccessHandler)
                        .failureUrl("/login?error=true")
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/?logout=true")
                    .permitAll())
                .exceptionHandling(exception -> exception
                    .accessDeniedHandler(customAccessDeniedHandler));

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
