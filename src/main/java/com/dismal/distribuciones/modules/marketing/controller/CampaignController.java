package com.dismal.distribuciones.modules.marketing.controller;

import com.dismal.distribuciones.modules.marketing.domain.CampaignStatus;
import com.dismal.distribuciones.modules.marketing.dto.CampaignCreateRequest;
import com.dismal.distribuciones.modules.marketing.dto.CampaignResponse;
import com.dismal.distribuciones.modules.marketing.dto.CampaignScheduleRequest;
import com.dismal.distribuciones.modules.marketing.dto.CampaignSendTestRequest;
import com.dismal.distribuciones.modules.marketing.dto.CampaignUpdateRequest;
import com.dismal.distribuciones.modules.marketing.service.CampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/marketing/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    @PostMapping
    public ResponseEntity<CampaignResponse> createCampaign(@Valid @RequestBody CampaignCreateRequest request) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        CampaignResponse createdCampaign = campaignService.createCampaign(request);
        return new ResponseEntity<>(createdCampaign, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<CampaignResponse>> getAllCampaigns(
            @RequestParam(required = false) CampaignStatus status,
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
        return ResponseEntity.ok(campaignService.listCampaigns(status, page, size));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CampaignResponse> updateCampaign(@PathVariable UUID id, @Valid @RequestBody CampaignUpdateRequest request) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        CampaignResponse updatedCampaign = campaignService.updateCampaign(id, request);
        return ResponseEntity.ok(updatedCampaign);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCampaign(@PathVariable UUID id) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        campaignService.deleteCampaign(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/schedule")
    public ResponseEntity<CampaignResponse> scheduleCampaign(
            @PathVariable UUID id,
            @RequestBody(required = false) CampaignScheduleRequest request
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(campaignService.scheduleCampaign(id, request));
    }

    @PostMapping("/{id}/send-test")
    public ResponseEntity<Void> sendTest(@PathVariable UUID id, @RequestBody CampaignSendTestRequest request) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        campaignService.sendTest(id, request);
        return ResponseEntity.accepted().build();
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
