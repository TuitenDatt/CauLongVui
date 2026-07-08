package com.example.CauLongVui.config;

import com.example.CauLongVui.entity.Category;
import com.example.CauLongVui.entity.Court;
import com.example.CauLongVui.entity.Court.CourtStatus;
import com.example.CauLongVui.entity.Product;
import com.example.CauLongVui.entity.User;
import com.example.CauLongVui.entity.User.Role;
import com.example.CauLongVui.repository.CategoryRepository;
import com.example.CauLongVui.repository.CourtRepository;
import com.example.CauLongVui.repository.ProductRepository;
import com.example.CauLongVui.repository.UserRepository;
import com.example.CauLongVui.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CourtRepository courtRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.example.CauLongVui.repository.MembershipPlanRepository planRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {

        // 1. Tao tai khoan Admin mac dinh neu chua co
        if (!userRepository.existsByEmail("admin@gmail.com")) {
            User admin = User.builder()
                    .fullName("Quan Tri Vien")
                    .email("admin@gmail.com")
                    .password(passwordEncoder.encode("admin@123"))
                    .phone("0999888777")
                    .role(Role.ADMIN)
                    .active(true)
                    .build();
            userRepository.save(admin);
            log.info("Admin account created: admin@gmail.com");
        } else {
            log.info("Admin account already exists.");
        }

        // 2. Tao du lieu san mau neu chua co
        if (courtRepository.count() == 0) {
            log.info("No courts found. Seeding sample courts...");

            List<Court> courts = List.of(
                    Court.builder()
                            .name("San A1")
                            .description("San tieu chuan tang 1 - Anh sang tot, thong thoang")
                            .pricePerHour(90000.0)
                            .status(CourtStatus.AVAILABLE)
                            .build(),
                    Court.builder()
                            .name("San A2")
                            .description("San tieu chuan tang 1 - Gan cua ra vao")
                            .pricePerHour(90000.0)
                            .status(CourtStatus.AVAILABLE)
                            .build(),
                    Court.builder()
                            .name("San A3")
                            .description("San tieu chuan tang 1 - San goc yen tinh")
                            .pricePerHour(90000.0)
                            .status(CourtStatus.AVAILABLE)
                            .build(),
                    Court.builder()
                            .name("San B1")
                            .description("San VIP tang 2 - Dieu hoa, ghe ngoi thoai mai")
                            .pricePerHour(150000.0)
                            .status(CourtStatus.AVAILABLE)
                            .build(),
                    Court.builder()
                            .name("San B2")
                            .description("San VIP tang 2 - View dep nhin ra san ngoai")
                            .pricePerHour(150000.0)
                            .status(CourtStatus.AVAILABLE)
                            .build());

            courtRepository.saveAll(courts);
            log.info("Seeded {} courts successfully.", courts.size());
        } else {
            log.info("Courts already exist ({}). Skipping seed.", courtRepository.count());
        }

        // 3. Tao goi membership mac dinh
        if (planRepository.count() == 0) {
            log.info("No membership plans found. Seeding default plans...");

            List<com.example.CauLongVui.entity.MembershipPlan> plans = List.of(
                    com.example.CauLongVui.entity.MembershipPlan.builder()
                            .name("Hội viên Thường")
                            .tier(com.example.CauLongVui.entity.MembershipTier.NORMAL)
                            .price(0L)
                            .durationInDays(36500)
                            .description("Gói mặc định cho người dùng mới.")
                            .build(),
                    com.example.CauLongVui.entity.MembershipPlan.builder()
                            .name("Hội viên Pro")
                            .tier(com.example.CauLongVui.entity.MembershipTier.PRO)
                            .price(50000L)
                            .durationInDays(30)
                            .description("Nhiều ưu đãi và giảm giá đặt sân.")
                            .build(),
                    com.example.CauLongVui.entity.MembershipPlan.builder()
                            .name("Hội viên VIP")
                            .tier(com.example.CauLongVui.entity.MembershipTier.VIP)
                            .price(150000L)
                            .durationInDays(30)
                            .description("Ưu tiên đặt sân và giảm giá tối đa.")
                            .build());

            planRepository.saveAll(plans);
            log.info("Seeded {} membership plans successfully.", plans.size());
        }

        seedDefaultCategories();
        migrateProductCategories();
    }

    private void seedDefaultCategories() {
        List<String> defaults = List.of(
                "C\u1ea7u l\u00f4ng",
                "V\u1ee3t",
                "Gi\u00e0y",
                "T\u00fai",
                "Qu\u1ea7n \u00e1o",
                "Ph\u1ee5 ki\u1ec7n",
                "N\u01b0\u1edbc u\u1ed1ng",
                "\u0110\u1ed3 \u0103n"
        );

        Map<String, Category> categoriesByName = new HashMap<>();
        categoryRepository.findAll().forEach(category ->
                categoriesByName.put(categoryKey(category.getName()), category));

        int order = 10;
        int created = 0;
        for (String name : defaults) {
            if (!categoriesByName.containsKey(categoryKey(name))) {
                Category category = categoryRepository.save(Category.builder()
                        .name(name)
                        .slug(uniqueCategorySlug(CategoryService.slugify(name)))
                        .active(true)
                        .sortOrder(order)
                        .build());
                categoriesByName.put(categoryKey(category.getName()), category);
                created++;
            }
            order += 10;
        }

        if (created > 0) {
            log.info("Seeded {} default categories.", created);
        }
    }

    private void migrateProductCategories() {
        List<Product> products = productRepository.findAll();
        Map<String, Category> categoriesByName = new HashMap<>();
        categoryRepository.findAll().forEach(category ->
                categoriesByName.put(categoryKey(category.getName()), category));
        int migrated = 0;

        for (Product product : products) {
            if (product.getCategoryEntity() != null) {
                continue;
            }

            String categoryName = normalizeLegacyCategory(product.getCategory());
            if (categoryName == null) {
                continue;
            }

            Category category = categoriesByName.computeIfAbsent(categoryKey(categoryName), key ->
                    categoryRepository.save(Category.builder()
                            .name(categoryName)
                            .slug(uniqueCategorySlug(CategoryService.slugify(categoryName)))
                            .active(true)
                            .sortOrder(0)
                            .build()));

            product.setCategoryEntity(category);
            product.setCategory(category.getName());
            migrated++;
        }

        if (migrated > 0) {
            productRepository.saveAll(products);
            log.info("Migrated {} product category references.", migrated);
        }
    }

    private String normalizeLegacyCategory(String rawCategory) {
        if (rawCategory == null || rawCategory.trim().isEmpty()) {
            return null;
        }

        String category = rawCategory.trim().replaceAll("\\s+", " ");
        String lower = category.toLowerCase();

        if (lower.contains("?? ?n") || lower.contains("do an") || lower.contains("đồ ăn")) {
            return "Đồ ăn";
        }
        if (lower.contains("n??c") || lower.contains("nuoc") || lower.contains("nước")) {
            return "Nước uống";
        }
        if (lower.contains("ph?") || lower.contains("phu kien") || lower.contains("phụ kiện")) {
            return "Phụ kiện";
        }
        if (lower.contains("c?u") || lower.contains("cầu")) {
            return "Cầu lông";
        }
        if (lower.contains("vợt") || lower.contains("vot")) {
            return "Vợt";
        }
        return category;
    }

    private String categoryKey(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private String uniqueCategorySlug(String baseSlug) {
        String slug = baseSlug == null || baseSlug.isBlank() ? "category" : baseSlug;
        String candidate = slug;
        int suffix = 2;
        while (categoryRepository.existsBySlug(candidate)) {
            candidate = slug + "-" + suffix++;
        }
        return candidate;
    }
}
