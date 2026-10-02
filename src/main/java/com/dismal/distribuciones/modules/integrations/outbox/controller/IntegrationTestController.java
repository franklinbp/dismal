package com.dismal.distribuciones.modules.integrations.outbox.controller;

import com.dismal.distribuciones.modules.integrations.outbox.dto.OutboxResponse;
import com.dismal.distribuciones.modules.integrations.outbox.dto.OutboxTestRequest;
import com.dismal.distribuciones.modules.integrations.outbox.service.OutboxAdminService;
import com.dismal.distribuciones.modules.integrations.outbox.service.OutboxDispatcher;
import com.dismal.distribuciones.modules.integrations.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/integrations/n8n")
@RequiredArgsConstructor
public class IntegrationTestController {

    private final OutboxService outboxService;
    private final OutboxAdminService outboxAdminService;
    private final OutboxDispatcher outboxDispatcher;

    @PostMapping("/test")
    public ResponseEntity<OutboxResponse> testIntegration(@RequestBody OutboxTestRequest request) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrOperator(authentication)) {
            return ResponseEntity.status(403).build();
        }

        UUID outboxId = outboxService.enqueueTestEvent(request.to(), request.message());
        outboxDispatcher.dispatchNow(outboxId);
        return ResponseEntity.ok(outboxAdminService.get(outboxId));
    }

    private Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication;
    }

    private boolean isAdminOrOperator(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ADMIN")
                        || authority.getAuthority().equals("OPERATOR"));
    }
}
