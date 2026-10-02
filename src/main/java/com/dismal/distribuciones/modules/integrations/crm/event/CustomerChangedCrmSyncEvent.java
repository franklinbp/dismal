package com.dismal.distribuciones.modules.integrations.crm.event;

import org.springframework.context.ApplicationEvent;

import java.util.UUID;

public class CustomerChangedCrmSyncEvent extends ApplicationEvent {

    private final UUID userId;

    public CustomerChangedCrmSyncEvent(Object source, UUID userId) {
        super(source);
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }
}
