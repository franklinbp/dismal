package com.dismal.distribuciones.modules.integrations.crm.controller;

import com.dismal.distribuciones.modules.integrations.crm.dto.CrmIntegrationStatusResponse;
import com.dismal.distribuciones.modules.integrations.crm.service.CrmIntegrationStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integrations/crm")
@RequiredArgsConstructor
public class CrmIntegrationStatusController {

    private final CrmIntegrationStatusService statusService;

    @GetMapping("/status")
    public ResponseEntity<CrmIntegrationStatusResponse> getStatus() {
        return ResponseEntity.ok(statusService.getStatus());
    }
}
