package com.dismal.distribuciones.modules.marketing.controller;

import com.dismal.distribuciones.modules.marketing.domain.StrategyActionStatus;
import com.dismal.distribuciones.modules.marketing.dto.StrategyActionCreateRequest;
import com.dismal.distribuciones.modules.marketing.dto.StrategyActionResponse;
import com.dismal.distribuciones.modules.marketing.dto.StrategyActionStatusRequest;
import com.dismal.distribuciones.modules.marketing.dto.StrategyActionUpdateRequest;
import com.dismal.distribuciones.modules.marketing.service.StrategyActionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/marketing/strategy/actions")
@RequiredArgsConstructor
public class StrategyActionController {

    private final StrategyActionService strategyActionService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<Page<StrategyActionResponse>> list(
            @RequestParam(required = false) StrategyActionStatus status,
            @RequestParam(required = false) UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(strategyActionService.list(status, productId, page, size));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<StrategyActionResponse> create(@Valid @RequestBody StrategyActionCreateRequest request) {
        return new ResponseEntity<>(strategyActionService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<StrategyActionResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody StrategyActionUpdateRequest request
    ) {
        return ResponseEntity.ok(strategyActionService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<StrategyActionResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StrategyActionStatusRequest request
    ) {
        return ResponseEntity.ok(strategyActionService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> discard(@PathVariable UUID id) {
        strategyActionService.discard(id);
        return ResponseEntity.noContent().build();
    }
}
