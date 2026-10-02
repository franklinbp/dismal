package com.dismal.desktop;

public record IntegrationSettingsUpdate(
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
        String smtpFromName
) {}
