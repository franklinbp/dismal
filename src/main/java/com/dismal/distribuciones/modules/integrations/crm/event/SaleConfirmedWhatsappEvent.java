package com.dismal.distribuciones.modules.integrations.crm.event;

import org.springframework.context.ApplicationEvent;

import java.util.UUID;

public class SaleConfirmedWhatsappEvent extends ApplicationEvent {

    private final String phone;
    private final String message;
    private final UUID outboxEventId;

    public SaleConfirmedWhatsappEvent(Object source, String phone, String message, UUID outboxEventId) {
        super(source);
        this.phone = phone;
        this.message = message;
        this.outboxEventId = outboxEventId;
    }

    public String getPhone() {
        return phone;
    }

    public String getMessage() {
        return message;
    }

    public UUID getOutboxEventId() {
        return outboxEventId;
    }
}
