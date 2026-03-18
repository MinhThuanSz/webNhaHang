package com.example.restaurantpro.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.restaurantpro.model.DiningTable;
import com.example.restaurantpro.model.MenuCategory;
import com.example.restaurantpro.model.MenuItem;
import com.example.restaurantpro.model.PaymentMethod;
import com.example.restaurantpro.model.PaymentStatus;
import com.example.restaurantpro.model.RoleName;
import com.example.restaurantpro.service.AppUserService;
import com.example.restaurantpro.service.BookingService;
import com.example.restaurantpro.service.MenuService;
import com.example.restaurantpro.service.TableService;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner seedData(AppUserService appUserService,
                                      TableService tableService,
                                      MenuService menuService,
                                      BookingService bookingService) {
        return args -> {
            appUserService.createSeedUser("Admin", "888888", "123456", Set.of(RoleName.ROLE_ADMIN));
            appUserService.createSeedUser("QuanLi", "0808080", "123456", Set.of(RoleName.ROLE_TABLE_MANAGER));
            appUserService.createSeedUser("Menu", "000000", "123456", Set.of(RoleName.ROLE_MENU_MANAGER));
            appUserService.createSeedUser("User", "111111", "123456", Set.of(RoleName.ROLE_CUSTOMER));

            if (tableService.countTables() == 0) {
                tableService.save(new DiningTable("Ban Lotus 01", "Ban tron", "Ghe tua", 5,
                        "Khu vuc cua kinh, phu hop nhom gia dinh nho va khach tiep doi tac.", "Tang 1", true));
                tableService.save(new DiningTable("Ban Sakura 02", "Ban dai", "Ghe boc nem", 8,
                        "Khong gian rieng tu, phu hop tiec sinh nhat hoac nhom ban.", "Phong VIP", true));
                tableService.save(new DiningTable("Ban Aurora 03", "Ban tron", "Ghe cao cap", 10,
                        "Khu vuc trung tam sanh, phu hop khach doan va tiec doanh nghiep.", "Sanh chinh", true));
                tableService.save(new DiningTable("Ban Terrace 04", "Ban ngoai troi", "Ghe may", 4,
                        "Ban cong thoang dang, phu hop cap doi hoac nhom nho.", "San vuon", true));
            }

            if (menuService.countMenuItems() == 0) {
                menuService.save(new MenuItem("Bo ap chao sot tieu den", MenuCategory.MAIN, "/images/steak.svg",
                        "Thit bo mem, ap chao vua chin toi, dung kem khoai nghien va rau cu nuong.", new BigDecimal("285000"), true));
                menuService.save(new MenuItem("Salad ca hoi xong khoi", MenuCategory.SALAD, "/images/salad.svg",
                        "Salad tuoi gion voi ca hoi xong khoi, sot chanh day thanh mat.", new BigDecimal("165000"), true));
                menuService.save(new MenuItem("Lau hai san dac biet", MenuCategory.HOTPOT, "/images/hotpot.svg",
                        "Noi lau dam vi voi tom, muc, ca va rau theo mua.", new BigDecimal("420000"), true));
                menuService.save(new MenuItem("Panna cotta dau tay", MenuCategory.DESSERT, "/images/dessert.svg",
                        "Mon trang mieng mem min, vi kem sua nhe, phu sot dau tuoi.", new BigDecimal("89000"), true));
                menuService.save(new MenuItem("Mi Y sot kem nam", MenuCategory.EUROPEAN, "/images/european.svg",
                        "Mi Y tuoi voi sot kem nam beo nhe va pho mai Parmesan.", new BigDecimal("195000"), true));
                menuService.save(new MenuItem("Com cuon rong bien ca ngu", MenuCategory.ASIAN, "/images/asian.svg",
                        "Mon A nhe nhang, vi thanh va phu hop goi kem truoc bua chinh.", new BigDecimal("125000"), true));
            }

            bookingService.getAllBookings().forEach(existingBooking -> {
                boolean changed = false;
                if (existingBooking.getPaymentMethod() == null) {
                    existingBooking.setPaymentMethod(PaymentMethod.PAY_AT_RESTAURANT);
                    changed = true;
                }
                if (existingBooking.getPaymentStatus() == null) {
                    existingBooking.setPaymentStatus(PaymentStatus.UNPAID);
                    changed = true;
                }
                if (changed) {
                    bookingService.save(existingBooking);
                }
            });

            if (bookingService.countBookings() == 0) {
                bookingService.createBooking(
                        "0988000004",
                        tableService.getAllTables().get(0).getId(),
                        4,
                        LocalDateTime.now().plusDays(1).withHour(19).withMinute(0),
                        "Khach muon ngoi gan cua so.",
                        Map.of(
                                menuService.findAllAvailable().get(0).getId(), 2,
                                menuService.findAllAvailable().get(3).getId(), 4
                        ),
                        PaymentMethod.PAY_AT_RESTAURANT
                );
            }
        };
    }
}
