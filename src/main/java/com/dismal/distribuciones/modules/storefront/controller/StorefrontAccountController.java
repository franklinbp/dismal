package com.dismal.distribuciones.modules.storefront.controller;

import com.dismal.distribuciones.modules.storefront.dto.StorefrontOrderPageResponse;
import com.dismal.distribuciones.modules.storefront.dto.StorefrontOrderResponse;
import com.dismal.distribuciones.modules.storefront.dto.StorefrontPaymentClaimRequest;
import com.dismal.distribuciones.modules.storefront.service.StorefrontAccountService;
import com.dismal.distribuciones.modules.storefront.service.StorefrontPaymentService;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/storefront/account")
@RequiredArgsConstructor
public class StorefrontAccountController {

    private final StorefrontAccountService storefrontAccountService;
    private final StorefrontPaymentService storefrontPaymentService;

    @GetMapping("/orders")
    public ResponseEntity<StorefrontOrderPageResponse> listOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "EC") StorefrontCountry country,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(storefrontAccountService.listOrders(
                authentication.getName(), country, page, size
        ));
    }

    @PostMapping("/orders/{orderNumber}/payment-claim")
    public ResponseEntity<StorefrontOrderResponse> submitPaymentClaim(
            @PathVariable String orderNumber,
            @Valid @RequestBody StorefrontPaymentClaimRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(storefrontPaymentService.submitPaymentClaim(
                orderNumber,
                authentication.getName(),
                request
        ));
    }

    @PostMapping("/orders/{orderNumber}/cancel")
    public ResponseEntity<StorefrontOrderResponse> cancelOrder(
            @PathVariable String orderNumber,
            Authentication authentication
    ) {
        return ResponseEntity.ok(storefrontAccountService.cancelOrder(
                authentication.getName(),
                orderNumber
        ));
    }
}
