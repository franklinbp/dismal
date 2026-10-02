package com.dismal.distribuciones.modules.reports.service;

import com.dismal.distribuciones.exception.BadRequestException;
import com.dismal.distribuciones.modules.integrations.crm.service.CrmWhatsappLineResolverService;
import com.dismal.distribuciones.modules.integrations.crm.service.DismalCrmGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceListWhatsappService {

    private final DismalCrmGateway gateway;
    private final CrmWhatsappLineResolverService lineResolverService;

    @Value("${integrations.dismal-crm.whatsapp-enabled:false}")
    private boolean whatsappEnabled;

    public void sendPriceList(String phone, String text) {
        if (phone == null || phone.isBlank()) {
            throw new BadRequestException("Whatsapp phone is required.");
        }
        if (text == null || text.isBlank()) {
            throw new BadRequestException("Whatsapp text is empty.");
        }
        if (!lineResolverService.isWhatsappEnabled(whatsappEnabled)) {
            throw new BadRequestException("DismalCRM WhatsApp integration is disabled.");
        }

        try {
            gateway.sendWhatsAppMessage(new DismalCrmGateway.SendWhatsAppMessageRequest(
                    phone,
                    text,
                    lineResolverService.resolveWhatsappId(phone)
            ));
        } catch (Exception ex) {
            log.warn("Failed to send price list through DismalCRM to {}: {}", phone, ex.getMessage());
            throw new BadRequestException("Failed to send WhatsApp through DismalCRM: " + ex.getMessage());
        }
    }
}
