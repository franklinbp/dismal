package com.dismal.distribuciones.modules.pricing.controller;

import com.dismal.distribuciones.modules.pricing.dto.ProductPricingResponse;
import com.dismal.distribuciones.modules.pricing.dto.UpdateProductPriceRequest;
import com.dismal.distribuciones.modules.pricing.service.PricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pricing/products")
@RequiredArgsConstructor
public class AdminPricingController {

    private final PricingService pricingService;

    @GetMapping("/{softwareId}")
    public ResponseEntity<ProductPricingResponse> getProductPricing(@PathVariable UUID softwareId) {
        return ResponseEntity.ok(pricingService.getProductPricing(softwareId));
    }

    @PatchMapping("/{softwareId}")
    public ResponseEntity<ProductPricingResponse> updateProductPricing(
            @PathVariable UUID softwareId,
            @Valid @RequestBody UpdateProductPriceRequest request
    ) {
        pricingService.upsertPrice(softwareId, request.type(), request.price());
        return ResponseEntity.ok(pricingService.getProductPricing(softwareId));
    }
}
