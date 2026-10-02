package com.dismal.distribuciones.modules.store.controller;

import com.dismal.distribuciones.modules.store.domain.License;
import com.dismal.distribuciones.modules.store.domain.Software;
import com.dismal.distribuciones.modules.store.dto.SoftwarePriceUpdateRequest;
import com.dismal.distribuciones.modules.pricing.dto.ProductPricingResponse;
import com.dismal.distribuciones.modules.pricing.service.PricingService;
import com.dismal.distribuciones.modules.store.service.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/store")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final PricingService pricingService;

    /**
     * DTO (Data Transfer Object) for the license creation request.
     * Using a Java Record for a concise, immutable data carrier.
     */
    public record LicenseRequest(
            UUID softwareId,
            String licenseKey,
            BigDecimal purchasePrice,
            Integer maxActivations
    ) {}

    public record SoftwareResponse(
            UUID id,
            String name,
            String description,
            BigDecimal price,
            String platform,
            String imageUrl,
            BigDecimal ecFinalPrice,
            BigDecimal ecDistributorPrice,
            BigDecimal peFinalPrice,
            BigDecimal peDistributorPrice
    ) {
        public static SoftwareResponse from(Software software, ProductPricingResponse pricing) {
            return new SoftwareResponse(
                    software.getId(),
                    software.getName(),
                    software.getDescription() != null ? software.getDescription() : "",
                    software.getPrice(),
                    software.getPlatform(),
                    software.getImageUrl(),
                    pricing.ecFinalPrice(),
                    pricing.ecDistributorPrice(),
                    pricing.peFinalPrice(),
                    pricing.peDistributorPrice()
            );
        }
    }

    public record SoftwareRequest(
            String name,
            String description,
            BigDecimal price,
            String platform,
            String imageUrl,
            BigDecimal ecFinalPrice,
            BigDecimal ecDistributorPrice,
            BigDecimal peFinalPrice,
            BigDecimal peDistributorPrice
    ) {
        public Software toSoftware() {
            return Software.builder()
                    .name(name)
                    .description(description)
                    .price(ecFinalPrice != null ? ecFinalPrice : price)
                    .platform(platform)
                    .imageUrl(imageUrl)
                    .build();
        }
    }

    @PostMapping("/product")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<SoftwareResponse> createSoftware(@RequestBody SoftwareRequest request) {
        Software savedSoftware = storeService.saveSoftware(
                request.toSoftware(),
                request.ecFinalPrice() != null ? request.ecFinalPrice() : request.price(),
                request.ecDistributorPrice(),
                request.peFinalPrice(),
                request.peDistributorPrice()
        );
        return new ResponseEntity<>(toResponse(savedSoftware), HttpStatus.CREATED);
    }

    @PostMapping("/products")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<SoftwareResponse> createSoftwareV2(@RequestBody SoftwareRequest request) {
        return createSoftware(request);
    }

    @GetMapping("/products")
    public ResponseEntity<List<SoftwareResponse>> getAllSoftware() {
        List<SoftwareResponse> softwareList = storeService.getAllSoftware().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(softwareList);
    }

    @PostMapping("/license")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<License> addLicense(@RequestBody LicenseRequest request) {
        // Convert DTO to entity
        License newLicense = License.builder()
                .licenseKey(request.licenseKey())
                .purchasePrice(request.purchasePrice())
                .maxActivations(request.maxActivations())
                .build();

        License savedLicense = storeService.addLicense(request.softwareId(), newLicense);
        return new ResponseEntity<>(savedLicense, HttpStatus.CREATED);
    }

    @PutMapping("/product/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<SoftwareResponse> updateSoftware(@PathVariable UUID id, @RequestBody SoftwareRequest request) {
        Software updatedSoftware = storeService.updateSoftware(
                id,
                request.toSoftware(),
                request.ecFinalPrice() != null ? request.ecFinalPrice() : request.price(),
                request.ecDistributorPrice(),
                request.peFinalPrice(),
                request.peDistributorPrice()
        );
        return ResponseEntity.ok(toResponse(updatedSoftware));
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<SoftwareResponse> updateSoftwareV2(@PathVariable UUID id, @RequestBody SoftwareRequest request) {
        return updateSoftware(id, request);
    }

    @DeleteMapping("/product/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteSoftware(@PathVariable UUID id,
                                               @RequestParam(defaultValue = "false") boolean force) {
        if (force) {
            storeService.forceDeleteSoftware(id);
        } else {
            storeService.deleteSoftware(id);
        }
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/product/{id}/price")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<SoftwareResponse> updateSoftwarePrice(@PathVariable UUID id, @Valid @RequestBody SoftwarePriceUpdateRequest request) {
        Software updatedSoftware = storeService.updateSoftwarePrice(id, request);
        return ResponseEntity.ok(toResponse(updatedSoftware));
    }

    @DeleteMapping("/license/{id}/deactivate")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deactivateLicense(@PathVariable UUID id) {
        storeService.deactivateLicense(id);
        return ResponseEntity.noContent().build();
    }

    private SoftwareResponse toResponse(Software software) {
        return SoftwareResponse.from(software, pricingService.getProductPricing(software.getId()));
    }
}
