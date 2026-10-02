package com.dismal.distribuciones.modules.sales.controller;

import com.dismal.distribuciones.modules.sales.dto.PaymentAccountRequest;
import com.dismal.distribuciones.modules.sales.dto.PaymentAccountResponse;
import com.dismal.distribuciones.modules.sales.dto.PaymentAccountMovementPageResponse;
import com.dismal.distribuciones.modules.sales.dto.PaymentAccountUsageResponse;
import com.dismal.distribuciones.modules.sales.service.PaymentAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment-accounts")
@RequiredArgsConstructor
public class PaymentAccountController {

    private final PaymentAccountService paymentAccountService;

    @GetMapping
    public ResponseEntity<List<PaymentAccountResponse>> list(
            @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(includeInactive
                ? paymentAccountService.listAll()
                : paymentAccountService.listActive());
    }

    @GetMapping("/usage")
    public ResponseEntity<List<PaymentAccountUsageResponse>> usage(
            @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(paymentAccountService.listUsage(includeInactive));
    }

    @GetMapping("/movements")
    public ResponseEntity<PaymentAccountMovementPageResponse> movements(
            @RequestParam(required = false) UUID accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(paymentAccountService.listMovements(accountId, page, size));
    }

    @PostMapping
    public ResponseEntity<PaymentAccountResponse> create(@RequestBody PaymentAccountRequest request) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(paymentAccountService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentAccountResponse> update(
            @PathVariable java.util.UUID id,
            @RequestBody PaymentAccountRequest request
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(paymentAccountService.update(id, request));
    }

    private Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication;
    }

    private boolean isAdminOrManager(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ADMIN")
                        || authority.getAuthority().equals("MANAGER"));
    }
}
