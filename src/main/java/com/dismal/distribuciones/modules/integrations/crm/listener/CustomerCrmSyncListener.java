package com.dismal.distribuciones.modules.integrations.crm.listener;

import com.dismal.distribuciones.modules.integrations.crm.event.CustomerChangedCrmSyncEvent;
import com.dismal.distribuciones.modules.integrations.crm.service.CrmCustomerSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerCrmSyncListener {

    private final CrmCustomerSyncService crmCustomerSyncService;

    @Async
    @EventListener(CustomerChangedCrmSyncEvent.class)
    public void handleCustomerChanged(CustomerChangedCrmSyncEvent event) {
        try {
            crmCustomerSyncService.syncCustomer(event.getUserId());
        } catch (Exception ex) {
            log.warn("Failed to sync changed customer {} to DismalCRM: {}", event.getUserId(), ex.getMessage());
        }
    }
}
