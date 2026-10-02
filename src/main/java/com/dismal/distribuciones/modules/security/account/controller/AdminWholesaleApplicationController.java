package com.dismal.distribuciones.modules.security.account.controller;

import com.dismal.distribuciones.modules.security.account.domain.WholesaleApplicationStatus;
import com.dismal.distribuciones.modules.security.account.dto.WholesaleApplicationResponse;
import com.dismal.distribuciones.modules.security.account.dto.WholesaleApplicationReviewRequest;
import com.dismal.distribuciones.modules.security.account.service.WholesaleApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/wholesale-applications")
@RequiredArgsConstructor
public class AdminWholesaleApplicationController {

    private final WholesaleApplicationService wholesaleApplicationService;

    @GetMapping
    public ResponseEntity<Page<WholesaleApplicationResponse>> list(
            @RequestParam(required = false) WholesaleApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(wholesaleApplicationService.list(status, page, size));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<WholesaleApplicationResponse> review(
            @PathVariable UUID id,
            @Valid @RequestBody WholesaleApplicationReviewRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(wholesaleApplicationService.review(id, request, authentication.getName()));
    }
}
