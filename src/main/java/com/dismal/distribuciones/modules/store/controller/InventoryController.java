package com.dismal.distribuciones.modules.store.controller;

import com.dismal.distribuciones.modules.store.domain.LicenseStatus;
import com.dismal.distribuciones.modules.store.dto.BulkLicenseRequest;
import com.dismal.distribuciones.modules.store.dto.CreateLicenseRequest;
import com.dismal.distribuciones.modules.store.dto.LicenseAssignmentResponse;
import com.dismal.distribuciones.modules.store.dto.LicenseResponse;
import com.dismal.distribuciones.modules.store.dto.LicenseUpdateRequest;
import com.dismal.distribuciones.modules.store.dto.StockSummaryResponse;
import com.dismal.distribuciones.modules.store.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/licenses")
    public ResponseEntity<Page<LicenseResponse>> listLicenses(
            @RequestParam(required = false) UUID softwareId,
            @RequestParam(required = false) LicenseStatus status,
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
        return ResponseEntity.ok(inventoryService.listLicenses(softwareId, status, page, size));
    }

    @GetMapping("/licenses/{id}/assignments")
    public ResponseEntity<List<LicenseAssignmentResponse>> listLicenseAssignments(@PathVariable UUID id) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(inventoryService.listLicenseAssignments(id));
    }

    @PostMapping("/licenses")
    public ResponseEntity<LicenseResponse> createLicense(@RequestBody CreateLicenseRequest request) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(inventoryService.createLicense(request));
    }

    @PostMapping("/licenses/bulk")
    public ResponseEntity<List<LicenseResponse>> createLicensesBulk(@RequestBody BulkLicenseRequest request) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(inventoryService.createLicensesBulk(request));
    }

    @PatchMapping("/licenses/{id}")
    public ResponseEntity<LicenseResponse> updateLicense(
            @PathVariable UUID id,
            @RequestBody LicenseUpdateRequest request
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(inventoryService.updateLicense(id, request));
    }

    @DeleteMapping("/licenses/{id}")
    public ResponseEntity<Void> deleteLicense(@PathVariable UUID id) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        inventoryService.deleteLicense(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stock")
    public ResponseEntity<List<StockSummaryResponse>> getStockSummary() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(inventoryService.getStockSummary());
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
