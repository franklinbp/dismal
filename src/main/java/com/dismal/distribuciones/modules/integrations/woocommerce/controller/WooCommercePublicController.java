package com.dismal.distribuciones.modules.integrations.woocommerce.controller;

import com.dismal.distribuciones.exception.BadRequestException;
import com.dismal.distribuciones.modules.integrations.woocommerce.dto.WooCustomerSyncPageResponse;
import com.dismal.distribuciones.modules.integrations.woocommerce.dto.WooOrderPaidRequest;
import com.dismal.distribuciones.modules.integrations.woocommerce.dto.WooOrderPaidResponse;
import com.dismal.distribuciones.modules.integrations.woocommerce.service.WooCommerceIntegrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/integrations/woocommerce")
@RequiredArgsConstructor
public class WooCommercePublicController {

    private final WooCommerceIntegrationService wooCommerceIntegrationService;

    @Value("${integrations.woocommerce.api-key:}")
    private String apiKey;

    @PostMapping("/order-paid")
    public ResponseEntity<WooOrderPaidResponse> processPaidOrder(
            @RequestHeader(value = "X-Dismal-Integration-Key", required = false) String providedKey,
            @Valid @RequestBody WooOrderPaidRequest request
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BadRequestException("WooCommerce integration is not configured.");
        }
        if (providedKey == null || !apiKey.equals(providedKey)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(wooCommerceIntegrationService.processPaidOrder(request));
    }

    @GetMapping("/customers")
    public ResponseEntity<WooCustomerSyncPageResponse> listCustomers(
            @RequestHeader(value = "X-Dismal-Integration-Key", required = false) String providedKey,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "100") int size,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "true") boolean enabledOnly
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BadRequestException("WooCommerce integration is not configured.");
        }
        if (providedKey == null || !apiKey.equals(providedKey)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(wooCommerceIntegrationService.listCustomersForWooCommerce(page, size, enabledOnly));
    }
}
