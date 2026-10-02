package com.dismal.desktop;

import java.time.Instant;

public record IntegrationSettings(
        String id,
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
        Instant updatedAt
) {}
