package com.dismal.distribuciones.config;

import com.dismal.distribuciones.modules.security.domain.Role;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.repository.UserRepository;
import com.dismal.distribuciones.modules.store.domain.License;
import com.dismal.distribuciones.modules.store.domain.LicenseStatus;
import com.dismal.distribuciones.modules.store.domain.Software;
import com.dismal.distribuciones.modules.store.repository.LicenseRepository;
import com.dismal.distribuciones.modules.store.repository.SoftwareRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DevDataSeeder implements ApplicationRunner {

    private static final String ADMIN_EMAIL = "admin@dismal.local";
    private static final String ADMIN_PASSWORD = "Admin123!";

    private final UserRepository userRepository;
    private final SoftwareRepository softwareRepository;
    private final LicenseRepository licenseRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        if (!shouldSeed()) {
            return;
        }

        seedAdminUser();
        List<Software> software = seedSoftware();
        seedLicenses(software);
    }

    private boolean shouldSeed() {
        boolean isDevProfile = Arrays.asList(environment.getActiveProfiles()).contains("dev");
        boolean isProdProfile = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        boolean seedFlag = Boolean.parseBoolean(environment.getProperty("DISMAL_SEED", "false"));
        if (isProdProfile) {
            return false;
        }
        return isDevProfile || seedFlag;
    }

    private void seedAdminUser() {
        if (userRepository.findByEmail(ADMIN_EMAIL).isPresent()) {
            return;
        }

        User admin = User.builder()
                .firstname("Admin")
                .lastname("Dismal")
                .email(ADMIN_EMAIL)
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        userRepository.save(admin);
        log.info("Seeded admin user: {}", ADMIN_EMAIL);
    }

    private List<Software> seedSoftware() {
        List<SeedSoftware> seeds = List.of(
                new SeedSoftware("Dismal CRM", "Web", new BigDecimal("49.00"),
                        "CRM para gestion de clientes y ventas.", "https://example.com/img/crm.png"),
                new SeedSoftware("Dismal POS", "Windows", new BigDecimal("79.00"),
                        "Punto de venta para retail con inventario.", "https://example.com/img/pos.png"),
                new SeedSoftware("Dismal Analytics", "Web", new BigDecimal("99.00"),
                        "Analitica y reportes avanzados.", "https://example.com/img/analytics.png")
        );

        Map<String, Software> existingByName = new HashMap<>();
        for (Software s : softwareRepository.findAll()) {
            existingByName.put(s.getName(), s);
        }

        List<Software> results = new ArrayList<>();
        for (SeedSoftware seed : seeds) {
            Software existing = existingByName.get(seed.name());
            if (existing != null) {
                results.add(existing);
                continue;
            }

            Software created = Software.builder()
                    .name(seed.name())
                    .platform(seed.platform())
                    .price(seed.price())
                    .description(seed.description())
                    .imageUrl(seed.imageUrl())
                    .build();

            results.add(softwareRepository.save(created));
            log.info("Seeded software: {}", seed.name());
        }

        return results;
    }

    private void seedLicenses(List<Software> software) {
        boolean seedLicenses = Boolean.parseBoolean(environment.getProperty("DISMAL_SEED_LICENSES", "true"));
        if (!seedLicenses) {
            return;
        }

        Map<UUID, Long> existingBySoftware = new HashMap<>();
        for (License license : licenseRepository.findAll()) {
            UUID softwareId = license.getSoftware() != null ? license.getSoftware().getId() : null;
            if (softwareId != null) {
                existingBySoftware.put(softwareId, existingBySoftware.getOrDefault(softwareId, 0L) + 1);
            }
        }

        for (Software s : software) {
            long existingCount = existingBySoftware.getOrDefault(s.getId(), 0L);
            if (existingCount >= 2) {
                continue;
            }

            for (int i = 0; i < 2; i++) {
                License license = License.builder()
                        .software(s)
                        .licenseKey(generateLicenseKey(s.getName()))
                        .purchasePrice(seedPurchasePrice(s.getPrice()))
                        .maxActivations(3)
                        .usedActivations(0)
                        .status(LicenseStatus.ACTIVE)
                        .build();
                licenseRepository.save(license);
            }
            log.info("Seeded licenses for software: {}", s.getName());
        }
    }

    private BigDecimal seedPurchasePrice(BigDecimal salePrice) {
        if (salePrice == null) {
            return BigDecimal.ZERO;
        }
        return salePrice.multiply(new BigDecimal("0.50")).setScale(2, RoundingMode.HALF_UP);
    }

    private String generateLicenseKey(String softwareName) {
        String prefix = softwareName == null ? "MSK" : softwareName.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (prefix.length() > 8) {
            prefix = prefix.substring(0, 8);
        }
        return "DEV-" + prefix + "-" + UUID.randomUUID();
    }

    private record SeedSoftware(String name, String platform, BigDecimal price, String description, String imageUrl) {
    }
}
