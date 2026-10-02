package com.dismal.distribuciones.modules.sales.controller;

import com.dismal.distribuciones.modules.sales.domain.AccountsReceivableStatus;
import com.dismal.distribuciones.modules.sales.dto.AccountsReceivableClientSummaryResponse;
import com.dismal.distribuciones.modules.sales.dto.AccountsReceivablePaymentRequest;
import com.dismal.distribuciones.modules.sales.dto.AccountsReceivablePaymentResponse;
import com.dismal.distribuciones.modules.sales.dto.AccountsReceivableResponse;
import com.dismal.distribuciones.modules.sales.dto.AccountsReceivableReminderResponse;
import com.dismal.distribuciones.modules.sales.service.AccountsReceivableService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ar")
@RequiredArgsConstructor
public class AccountsReceivableController {

    private final AccountsReceivableService accountsReceivableService;

    @GetMapping
    public ResponseEntity<List<AccountsReceivableResponse>> listAll() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(accountsReceivableService.listAll());
    }

    @GetMapping("/grouped")
    public ResponseEntity<List<AccountsReceivableClientSummaryResponse>> listGrouped(
            @RequestParam(defaultValue = "OVERDUE") AccountsReceivableStatus status
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(accountsReceivableService.listGroupedByClient(status));
    }

    @GetMapping("/client/{clientId}/grouped")
    public ResponseEntity<AccountsReceivableClientSummaryResponse> getClientGrouped(
            @PathVariable UUID clientId,
            @RequestParam(defaultValue = "OVERDUE") AccountsReceivableStatus status
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        AccountsReceivableClientSummaryResponse response = accountsReceivableService.getClientDetail(clientId, status);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<AccountsReceivableResponse>> listByClient(@PathVariable UUID clientId) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(accountsReceivableService.listByClient(clientId));
    }

    @GetMapping("/sale/{saleId}")
    public ResponseEntity<AccountsReceivableResponse> getBySale(@PathVariable UUID saleId) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        AccountsReceivableResponse response = accountsReceivableService.getBySaleId(saleId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reminder")
    public ResponseEntity<AccountsReceivableReminderResponse> sendReminder(@PathVariable UUID id) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(accountsReceivableService.sendReminder(id));
    }

    @PostMapping("/client/{clientId}/reminder")
    public ResponseEntity<AccountsReceivableReminderResponse> sendClientReminder(
            @PathVariable UUID clientId,
            @RequestParam(defaultValue = "OVERDUE") AccountsReceivableStatus status
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(accountsReceivableService.sendClientReminder(clientId, status));
    }

    @PostMapping("/client/{clientId}/payments")
    public ResponseEntity<AccountsReceivablePaymentResponse> registerClientPayment(
            @PathVariable UUID clientId,
            @RequestBody AccountsReceivablePaymentRequest request
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        AccountsReceivableClientSummaryResponse summary = accountsReceivableService.registerClientPayment(clientId, request);
        return ResponseEntity.ok(new AccountsReceivablePaymentResponse(
                true,
                "Cobro registrado correctamente.",
                summary
        ));
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
