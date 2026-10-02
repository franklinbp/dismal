package com.dismal.distribuciones.modules.integrations.outbox.domain;

public enum EventOutboxStatus {
    PENDING,
    IN_PROGRESS,
    SENT,
    FAILED
}
