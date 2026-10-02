package com.dismal.distribuciones.modules.integrations.outbox.dto;

public record OutboxTestRequest(
        String to,
        String message
) {}
