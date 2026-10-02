package com.dismal.distribuciones.modules.sales.controller;

import com.dismal.distribuciones.modules.sales.domain.QuoteStatus;
import com.dismal.distribuciones.modules.sales.dto.CreateQuoteRequest;
import com.dismal.distribuciones.modules.sales.dto.QuoteResponse;
import com.dismal.distribuciones.modules.sales.dto.QuoteSummaryResponse;
import com.dismal.distribuciones.modules.sales.dto.SaleResponse;
import com.dismal.distribuciones.modules.sales.service.QuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/quotes")
@RequiredArgsConstructor
public class QuoteController {

    private final QuoteService quoteService;

    @PostMapping
    public ResponseEntity<QuoteResponse> createQuote(@RequestBody CreateQuoteRequest request) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(quoteService.createQuote(request));
    }

    @GetMapping
    public ResponseEntity<Page<QuoteSummaryResponse>> listQuotes(
            @RequestParam(required = false) QuoteStatus status,
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
        return ResponseEntity.ok(quoteService.listQuotes(status, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuoteResponse> getQuote(@PathVariable UUID id) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(quoteService.getQuote(id));
    }

    @PostMapping("/{id}/convert-to-sale")
    public ResponseEntity<SaleResponse> convertToSale(@PathVariable UUID id) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(quoteService.convertToSale(id));
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
