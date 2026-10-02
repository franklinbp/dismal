package com.dismal.distribuciones.modules.integrations.notifications.controller;

import com.dismal.distribuciones.modules.integrations.notifications.dto.NotificationDeliveryCallbackRequest;
import com.dismal.distribuciones.modules.integrations.notifications.dto.NotificationDeliveryLogResponse;
import com.dismal.distribuciones.modules.integrations.notifications.dto.NotificationTemplateRequest;
import com.dismal.distribuciones.modules.integrations.notifications.dto.NotificationTemplateResponse;
import com.dismal.distribuciones.modules.integrations.notifications.service.NotificationTemplateAdminService;
import com.dismal.distribuciones.modules.integrations.settings.service.IntegrationSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/integrations/notifications/templates")
@RequiredArgsConstructor
public class NotificationTemplateController {

    private final NotificationTemplateAdminService templateService;
    private final IntegrationSettingsService settingsService;

    @GetMapping
    public ResponseEntity<List<NotificationTemplateResponse>> listTemplates(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) Boolean enabled
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(templateService.list(eventType, channel, enabled));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationTemplateResponse> getTemplate(@PathVariable UUID id) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(templateService.get(id));
    }

    @PostMapping
    public ResponseEntity<NotificationTemplateResponse> createTemplate(@RequestBody NotificationTemplateRequest request) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(templateService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NotificationTemplateResponse> updateTemplate(
            @PathVariable UUID id,
            @RequestBody NotificationTemplateRequest request
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(templateService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        templateService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/delivery-logs")
    public ResponseEntity<List<NotificationDeliveryLogResponse>> listDeliveryLogs(
            @RequestParam UUID outboxEventId
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(templateService.listDeliveryLogs(outboxEventId));
    }

    @PostMapping("/delivery-callback")
    public ResponseEntity<NotificationDeliveryLogResponse> deliveryCallback(
            @RequestBody NotificationDeliveryCallbackRequest request,
            @RequestHeader(value = "X-Integration-Secret", required = false) String integrationSecret
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null || !isAdminOrManager(authentication)) {
            String secret = settingsService.getSettingsEntity().getSecret();
            if (secret == null || secret.isBlank() || !secret.equals(integrationSecret)) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(templateService.upsertDeliveryLog(request));
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
