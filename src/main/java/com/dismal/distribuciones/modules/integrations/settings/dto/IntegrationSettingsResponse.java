package com.dismal.distribuciones.modules.integrations.settings.dto;

import java.time.Instant;
import java.util.UUID;

public record IntegrationSettingsResponse(
        UUID id,
        String webhookUrl,
        boolean dispatchEnabled,
        long dispatchRateMs,
        int dispatchMaxAttempts,
        boolean hasSecret,
        boolean smtpEnabled,
        String smtpHost,
        Integer smtpPort,
        String smtpUser,
        boolean smtpTls,
        String smtpFromEmail,
        String smtpFromName,
        boolean hasSmtpPassword,
        boolean crmWhatsappEnabled,
        String crmWhatsappDefaultId,
        String crmWhatsappIdByCountry,
        Instant updatedAt
) {}
