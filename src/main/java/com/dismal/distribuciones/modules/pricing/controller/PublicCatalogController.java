package com.dismal.distribuciones.modules.pricing.controller;

import com.dismal.distribuciones.exception.ResourceNotFoundException;
import com.dismal.distribuciones.modules.pricing.domain.PriceListType;
import com.dismal.distribuciones.modules.pricing.dto.PublicProductResponse;
import com.dismal.distribuciones.modules.pricing.service.PricingService;
import com.dismal.distribuciones.modules.security.domain.CustomerType;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.repository.UserRepository;
import com.dismal.distribuciones.modules.security.customer.service.CustomerMarketService;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;
import com.dismal.distribuciones.modules.store.domain.Software;
import com.dismal.distribuciones.modules.store.repository.SoftwareRepository;
import com.dismal.distribuciones.modules.store.repository.LicenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/public/products")
@RequiredArgsConstructor
public class PublicCatalogController {

    private final SoftwareRepository softwareRepository;
    private final LicenseRepository licenseRepository;
    private final UserRepository userRepository;
    private final PricingService pricingService;
    private final CustomerMarketService customerMarketService;

    @GetMapping
    public ResponseEntity<List<PublicProductResponse>> listProducts(
            @RequestParam(defaultValue = "EC") String country
    ) {
        User customer = getCurrentUser();
        String countryCode = normalizeCountry(country);
        List<PublicProductResponse> response = softwareRepository.findAll().stream()
                .map(software -> toResponse(software, customer, countryCode))
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicProductResponse> getProduct(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "EC") String country
    ) {
        User customer = getCurrentUser();
        String countryCode = normalizeCountry(country);
        Software software = softwareRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Software not found with ID: " + id));
        return ResponseEntity.ok(toResponse(software, customer, countryCode));
    }

    private PublicProductResponse toResponse(
            Software software,
            User customer,
            String countryCode
    ) {
        BigDecimal publicPrice = software.getPrice();
        BigDecimal finalPrice = pricingService.resolvePrice(software, countryCode, CustomerType.FINAL);
        CustomerType effectiveCustomerType = resolveEffectiveCustomerType(customer, countryCode);
        BigDecimal wholesalePrice = effectiveCustomerType == CustomerType.DISTRIBUTOR
                ? pricingService.resolvePrice(software, countryCode, CustomerType.DISTRIBUTOR)
                : null;
        BigDecimal effectivePrice = wholesalePrice != null ? wholesalePrice : finalPrice;
        PriceListType priceType = PriceListType.from(countryCode, effectiveCustomerType);
        boolean physicalProduct = Boolean.TRUE.equals(software.getPhysicalProduct());
        int availableQuantity = physicalProduct
                ? Math.max(0, valueOrZero(software.getStockQuantity()) - valueOrZero(software.getReservedQuantity()))
                : Math.toIntExact(Math.min(Integer.MAX_VALUE, licenseRepository.countAvailableActivations(software.getId())));
        return new PublicProductResponse(
                software.getId(),
                software.getName(),
                software.getDescription() != null ? software.getDescription() : "",
                software.getPlatform(),
                software.getImageUrl(),
                software.getSku(),
                software.getBrand(),
                physicalProduct,
                availableQuantity,
                availableQuantity > 0,
                publicPrice,
                effectivePrice,
                finalPrice,
                wholesalePrice,
                priceType
        );
    }

    private String normalizeCountry(String country) {
        if (country == null || country.isBlank()) {
            return "EC";
        }
        String normalized = country.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("PE") ? "PE" : "EC";
    }

    private int valueOrZero(Integer value) {
        return value != null ? value : 0;
    }

    private CustomerType resolveEffectiveCustomerType(User customer, String countryCode) {
        return customerMarketService.resolveCustomerType(
                customer,
                StorefrontCountry.valueOf(countryCode)
        );
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userRepository.findByEmail(authentication.getName()).orElse(null);
    }
}
