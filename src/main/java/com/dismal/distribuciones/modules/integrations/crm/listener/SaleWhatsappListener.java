package com.dismal.distribuciones.modules.integrations.crm.listener;

import com.dismal.distribuciones.modules.integrations.crm.event.SaleConfirmedWhatsappEvent;
import com.dismal.distribuciones.modules.integrations.crm.service.CrmWhatsappNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class SaleWhatsappListener {

    private final CrmWhatsappNotificationService whatsappNotificationService;

    public SaleWhatsappListener(CrmWhatsappNotificationService whatsappNotificationService) {
        this.whatsappNotificationService = whatsappNotificationService;
    }

    @Async
    @TransactionalEventListener(
            classes = SaleConfirmedWhatsappEvent.class,
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void handleSaleConfirmedWhatsappEvent(SaleConfirmedWhatsappEvent event) {
        var result = whatsappNotificationService.sendMessage(event.getPhone(), event.getMessage(), event.getOutboxEventId());
        if (result.sent()) {
            log.info("Sent WhatsApp sale notification to {}", event.getPhone());
        } else {
            log.warn("Failed to send WhatsApp sale notification to {}: {}", event.getPhone(), result.message());
        }
    }
}
