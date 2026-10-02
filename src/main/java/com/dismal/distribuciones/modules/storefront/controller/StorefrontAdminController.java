package com.dismal.distribuciones.modules.storefront.controller;

import com.dismal.distribuciones.modules.storefront.dto.StorefrontOrderResponse;
import com.dismal.distribuciones.modules.storefront.dto.StorefrontPaymentConfirmationRequest;
import com.dismal.distribuciones.modules.storefront.dto.StorefrontPaymentReviewRequest;
import com.dismal.distribuciones.modules.storefront.dto.StorefrontShipmentRequest;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontOrderStatus;
import com.dismal.distribuciones.modules.storefront.service.StorefrontOrderService;
import com.dismal.distribuciones.modules.storefront.service.StorefrontPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/storefront/orders")
@RequiredArgsConstructor
public class StorefrontAdminController {

    private final StorefrontPaymentService storefrontPaymentService;
    private final StorefrontOrderService storefrontOrderService;

    @GetMapping
    public ResponseEntity<Page<StorefrontOrderResponse>> listOrders(
            @RequestParam(required = false) StorefrontOrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        return ResponseEntity.ok(storefrontOrderService.listOrders(status, page, size));
    }

    @PostMapping("/{orderNumber}/confirm-payment")
    public ResponseEntity<StorefrontOrderResponse> confirmPayment(
            @PathVariable String orderNumber,
            @Valid @RequestBody StorefrontPaymentConfirmationRequest request
    ) {
        return ResponseEntity.ok(storefrontPaymentService.confirmPayment(orderNumber, request));
    }

    @PostMapping("/{orderNumber}/review-payment")
    public ResponseEntity<StorefrontOrderResponse> reviewPayment(
            @PathVariable String orderNumber,
            @Valid @RequestBody StorefrontPaymentReviewRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(storefrontPaymentService.reviewPayment(
                orderNumber,
                request,
                authentication.getName()
        ));
    }

    @PostMapping("/{orderNumber}/ready")
    public ResponseEntity<StorefrontOrderResponse> markReady(@PathVariable String orderNumber) {
        return ResponseEntity.ok(storefrontOrderService.markReadyForDispatch(orderNumber));
    }

    @PostMapping("/{orderNumber}/ship")
    public ResponseEntity<StorefrontOrderResponse> ship(
            @PathVariable String orderNumber,
            @Valid @RequestBody StorefrontShipmentRequest request
    ) {
        return ResponseEntity.ok(storefrontOrderService.markShipped(
                orderNumber, request.carrier(), request.trackingNumber()));
    }

    @PostMapping("/{orderNumber}/deliver")
    public ResponseEntity<StorefrontOrderResponse> deliver(@PathVariable String orderNumber) {
        return ResponseEntity.ok(storefrontOrderService.markDelivered(orderNumber));
    }
}
