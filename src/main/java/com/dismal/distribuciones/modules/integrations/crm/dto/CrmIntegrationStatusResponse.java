package com.dismal.distribuciones.modules.integrations.crm.dto;

public record CrmIntegrationStatusResponse(
        boolean enabled,
        boolean configured,
        boolean whatsappEnabled,
        boolean reachable,
        String endpoint,
        String message
) {}
