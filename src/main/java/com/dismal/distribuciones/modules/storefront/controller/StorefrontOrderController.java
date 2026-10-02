package com.dismal.distribuciones.modules.storefront.controller;

import com.dismal.distribuciones.modules.storefront.dto.StorefrontOrderRequest;
import com.dismal.distribuciones.modules.storefront.dto.StorefrontOrderResponse;
import com.dismal.distribuciones.modules.storefront.service.StorefrontOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/storefront/orders")
@RequiredArgsConstructor
public class StorefrontOrderController {

    private final StorefrontOrderService storefrontOrderService;

    @PostMapping
    public ResponseEntity<StorefrontOrderResponse> createPendingOrder(
            @Valid @RequestBody StorefrontOrderRequest request,
            Authentication authentication
    ) {
        String authenticatedEmail = authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                ? authentication.getName()
                : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(storefrontOrderService.createPendingOrder(request, authenticatedEmail));
    }
}
