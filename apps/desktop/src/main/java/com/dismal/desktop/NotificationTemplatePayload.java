package com.dismal.desktop;

public record NotificationTemplatePayload(
        String eventType,
        String channel,
        String subject,
        String body,
        boolean enabled
) {}
