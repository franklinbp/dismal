package com.dismal.distribuciones.modules.integrations.outbox.controller;

import com.dismal.distribuciones.modules.integrations.outbox.domain.EventOutboxStatus;
import com.dismal.distribuciones.modules.integrations.outbox.dto.OutboxResponse;
import com.dismal.distribuciones.modules.integrations.outbox.service.OutboxAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/integrations/outbox")
@RequiredArgsConstructor
public class OutboxController {

    private final OutboxAdminService outboxAdminService;

    @GetMapping
    public ResponseEntity<Page<OutboxResponse>> listOutbox(
            @RequestParam(required = false) EventOutboxStatus status,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) UUID aggregateId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrOperatorOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(outboxAdminService.list(status, eventType, aggregateId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OutboxResponse> getOutbox(@PathVariable UUID id) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrOperatorOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(outboxAdminService.get(id));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<OutboxResponse> retryOutbox(@PathVariable UUID id) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrOperatorOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(outboxAdminService.retryOne(id));
    }

    @PostMapping("/retry-failed")
    public ResponseEntity<Integer> retryFailed(@RequestParam(required = false) String eventType) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrOperatorOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(outboxAdminService.retryFailed(eventType));
    }

    private Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication;
    }

    private boolean isAdminOrOperatorOrManager(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ADMIN")
                        || authority.getAuthority().equals("OPERATOR")
                        || authority.getAuthority().equals("MANAGER"));
    }
}
