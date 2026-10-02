package com.dismal.distribuciones.modules.dashboard.controller;

import com.dismal.distribuciones.modules.dashboard.dto.*;
import com.dismal.distribuciones.modules.dashboard.service.DashboardService;
import com.dismal.distribuciones.modules.integrations.outbox.domain.EventOutboxStatus;
import com.dismal.distribuciones.modules.sales.domain.AccountsReceivableStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard/admin")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> summary() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        try {
            return ResponseEntity.ok(dashboardService.getSummary());
        } catch (RuntimeException ex) {
            log.warn("Dashboard summary request failed", ex);
            return ResponseEntity.ok(DashboardSummaryResponse.empty());
        }
    }

    @GetMapping("/recent-sales")
    public ResponseEntity<Page<RecentSaleDto>> recentSales(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
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
        return ResponseEntity.ok(dashboardService.getRecentSales(from, to, page, size));
    }

    @GetMapping("/ar")
    public ResponseEntity<Page<ArDto>> accountsReceivable(
            @RequestParam AccountsReceivableStatus status,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
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
        return ResponseEntity.ok(dashboardService.getAr(status, from, to, page, size));
    }

    @GetMapping("/sales-targets")
    public ResponseEntity<Page<SalesTargetDto>> salesTargets(
            @RequestParam(defaultValue = "profit") String sort,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
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
        return ResponseEntity.ok(dashboardService.getSalesTargets(sort, from, to, page, size));
    }

    @GetMapping("/outbox")
    public ResponseEntity<Page<OutboxDto>> outbox(
            @RequestParam EventOutboxStatus status,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
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
        return ResponseEntity.ok(dashboardService.getOutbox(status, from, to, page, size));
    }

    @GetMapping("/monthly-sales")
    public ResponseEntity<java.util.Map<String, java.math.BigDecimal>> monthlySales(
            @RequestParam(required = false) Integer year
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        int resolvedYear = year != null ? year : java.time.LocalDate.now().getYear();
        return ResponseEntity.ok(dashboardService.getMonthlySales(resolvedYear));
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
