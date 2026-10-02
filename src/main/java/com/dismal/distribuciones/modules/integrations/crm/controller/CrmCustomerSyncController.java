package com.dismal.distribuciones.modules.integrations.crm.controller;

import com.dismal.distribuciones.modules.integrations.crm.dto.CrmCustomerSyncResponse;
import com.dismal.distribuciones.modules.integrations.crm.service.CrmCustomerSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integrations/crm/customers")
@RequiredArgsConstructor
public class CrmCustomerSyncController {

    private final CrmCustomerSyncService crmCustomerSyncService;

    @PostMapping("/sync")
    public ResponseEntity<CrmCustomerSyncResponse> syncCustomers() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(crmCustomerSyncService.syncAllCustomers());
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
