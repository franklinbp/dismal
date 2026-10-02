package com.dismal.distribuciones.modules.integrations.settings.dto;

public record IntegrationSettingsRequest(
        String webhookUrl,
        String secret,
        Boolean dispatchEnabled,
        Long dispatchRateMs,
        Integer dispatchMaxAttempts,
        Boolean smtpEnabled,
        String smtpHost,
        Integer smtpPort,
        String smtpUser,
        String smtpPassword,
        Boolean smtpTls,
        String smtpFromEmail,
        String smtpFromName,
        Boolean crmWhatsappEnabled,
        String crmWhatsappDefaultId,
        String crmWhatsappIdByCountry
) {}
