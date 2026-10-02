package com.dismal.distribuciones.modules.integrations.settings.controller;

import com.dismal.distribuciones.exception.BadRequestException;
import com.dismal.distribuciones.modules.integrations.notifications.service.EmailNotificationService;
import com.dismal.distribuciones.modules.integrations.settings.dto.IntegrationSettingsRequest;
import com.dismal.distribuciones.modules.integrations.settings.dto.IntegrationSettingsResponse;
import com.dismal.distribuciones.modules.integrations.settings.dto.IntegrationSettingsTestEmailRequest;
import com.dismal.distribuciones.modules.integrations.settings.dto.IntegrationSettingsTestEmailResponse;
import com.dismal.distribuciones.modules.integrations.settings.service.IntegrationSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/integrations/settings")
@RequiredArgsConstructor
public class IntegrationSettingsController {

    private final IntegrationSettingsService settingsService;
    private final EmailNotificationService emailNotificationService;

    @GetMapping
    public ResponseEntity<IntegrationSettingsResponse> getSettings() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @PutMapping
    public ResponseEntity<IntegrationSettingsResponse> updateSettings(@RequestBody IntegrationSettingsRequest request) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(settingsService.updateSettings(request));
    }

    @PostMapping("/test-email")
    public ResponseEntity<IntegrationSettingsTestEmailResponse> testEmail(
            @RequestBody IntegrationSettingsTestEmailRequest request
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        try {
            var smtpSettings = settingsService.resolveSmtpSettingsForTest(request);
            emailNotificationService.sendTestEmail(
                    request != null ? request.to() : null,
                    request != null ? request.subject() : null,
                    request != null ? request.body() : null,
                    smtpSettings
            );
            String to = request != null ? request.to() : null;
            return ResponseEntity.ok(new IntegrationSettingsTestEmailResponse("Test email sent.", to));
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            String detail = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
            throw new BadRequestException("SMTP test failed: " + detail);
        }
    }

    private Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
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
