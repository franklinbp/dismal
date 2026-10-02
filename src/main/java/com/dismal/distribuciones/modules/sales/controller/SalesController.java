package com.dismal.distribuciones.modules.sales.controller;

import com.dismal.distribuciones.exception.ResourceNotFoundException;
import com.dismal.distribuciones.modules.sales.domain.SaleStatus;
import com.dismal.distribuciones.modules.sales.domain.SaleType;
import com.dismal.distribuciones.modules.sales.dto.ConfirmSaleRequest;
import com.dismal.distribuciones.modules.sales.dto.CreateSaleRequest;
import com.dismal.distribuciones.modules.sales.dto.PaymentResponse;
import com.dismal.distribuciones.modules.sales.dto.SaleLicenseResponse;
import com.dismal.distribuciones.modules.sales.dto.SaleResponse;
import com.dismal.distribuciones.modules.sales.service.PaymentService;
import com.dismal.distribuciones.modules.sales.service.SalesService;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.repository.UserRepository;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;
    private final PaymentService paymentService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<SaleResponse> createSale(@RequestBody CreateSaleRequest request) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(salesService.createSale(request));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<SaleResponse> confirmSale(
            @PathVariable UUID id,
            @RequestBody(required = false) ConfirmSaleRequest request
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(salesService.confirmSale(id, request != null ? request.deliveryChannels() : null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SaleResponse> updateSale(@PathVariable UUID id, @RequestBody CreateSaleRequest request) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(salesService.updateSale(id, request));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<SaleResponse> cancelSale(@PathVariable UUID id) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(salesService.cancelSale(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSale(@PathVariable UUID id) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        salesService.deleteSale(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SaleResponse> getSale(@PathVariable UUID id) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        SaleResponse sale = salesService.getSale(id);
        if (!isAdminOrManager(authentication) && !isOwner(authentication, sale.clientId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(sale);
    }

    @GetMapping
    public ResponseEntity<Page<SaleResponse>> listSales(
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) SaleStatus status,
            @RequestParam(required = false) SaleType type,
            @RequestParam(required = false) StorefrontCountry country,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(salesService.listSales(
                clientId, status, type, country, from, to, page, size
        ));
    }

    @GetMapping("/{id}/payments")
    public ResponseEntity<List<PaymentResponse>> listPayments(@PathVariable UUID id) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        SaleResponse sale = salesService.getSale(id);
        if (!isAdminOrManager(authentication) && !isOwner(authentication, sale.clientId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(paymentService.listPayments(id));
    }

    @GetMapping("/{id}/licenses")
    public ResponseEntity<List<SaleLicenseResponse>> listLicenses(@PathVariable UUID id) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        SaleResponse sale = salesService.getSale(id);
        if (!isAdminOrManager(authentication) && !isOwner(authentication, sale.clientId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(salesService.listSaleLicenses(id));
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

    private boolean isOwner(Authentication authentication, UUID clientId) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getId().equals(clientId);
    }
}
