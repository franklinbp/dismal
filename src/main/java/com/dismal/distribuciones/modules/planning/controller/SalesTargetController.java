package com.dismal.distribuciones.modules.planning.controller;

import com.dismal.distribuciones.modules.planning.dto.SalesTargetRequest;
import com.dismal.distribuciones.modules.planning.dto.SalesTargetResponse;
import com.dismal.distribuciones.modules.planning.dto.SalesTargetSummaryResponse;
import com.dismal.distribuciones.modules.planning.dto.RealActivationCostResponse;
import com.dismal.distribuciones.modules.planning.dto.SalesTargetSettingsRequest;
import com.dismal.distribuciones.modules.planning.dto.SalesTargetSettingsResponse;
import com.dismal.distribuciones.modules.planning.domain.SalesTargetStatus;
import com.dismal.distribuciones.modules.planning.service.SalesTargetService;
import com.dismal.distribuciones.modules.planning.service.SalesTargetSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sales-targets")
@RequiredArgsConstructor
public class SalesTargetController {

    private final SalesTargetService salesTargetService;
    private final SalesTargetSettingsService settingsService;

    @PostMapping
    public ResponseEntity<SalesTargetResponse> create(@RequestBody SalesTargetRequest request) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(salesTargetService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SalesTargetResponse> update(@PathVariable UUID id, @RequestBody SalesTargetRequest request) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(salesTargetService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        salesTargetService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<SalesTargetResponse>> list(
            @RequestParam(required = false) SalesTargetStatus status
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isViewer(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(salesTargetService.list(status));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<SalesTargetResponse> close(@PathVariable UUID id) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(salesTargetService.close(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalesTargetResponse> get(@PathVariable UUID id) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isViewer(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(salesTargetService.get(id));
    }

    @GetMapping("/summary")
    public ResponseEntity<SalesTargetSummaryResponse> summary() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isViewer(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(salesTargetService.summary());
    }

    @GetMapping("/real-costs")
    public ResponseEntity<List<RealActivationCostResponse>> realCosts() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isViewer(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(salesTargetService.realActivationCosts());
    }

    @GetMapping("/settings")
    public ResponseEntity<SalesTargetSettingsResponse> getSettings() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @PutMapping("/settings")
    public ResponseEntity<SalesTargetSettingsResponse> updateSettings(@RequestBody SalesTargetSettingsRequest request) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(settingsService.updateSettings(request));
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

    private boolean isViewer(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ADMIN")
                        || authority.getAuthority().equals("MANAGER")
                        || authority.getAuthority().equals("USER"));
    }
}
