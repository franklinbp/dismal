package com.dismal.distribuciones.modules.integrations.settings.dto;

public record IntegrationSettingsTestEmailRequest(
        String to,
        String subject,
        String body,
        Boolean smtpEnabled,
        String smtpHost,
        Integer smtpPort,
        String smtpUser,
        String smtpPassword,
        Boolean smtpTls,
        String smtpFromEmail,
        String smtpFromName
) {}
