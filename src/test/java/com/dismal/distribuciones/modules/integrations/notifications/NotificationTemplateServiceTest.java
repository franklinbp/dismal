package com.dismal.distribuciones.modules.integrations.notifications;

import com.dismal.distribuciones.modules.integrations.notifications.domain.NotificationChannel;
import com.dismal.distribuciones.modules.integrations.notifications.domain.NotificationTemplate;
import com.dismal.distribuciones.modules.integrations.notifications.repository.NotificationTemplateRepository;
import com.dismal.distribuciones.modules.integrations.notifications.service.NotificationTemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class NotificationTemplateServiceTest {

    @Autowired
    private NotificationTemplateRepository templateRepository;

    @Autowired
    private NotificationTemplateService templateService;

    @Test
    void resolvesTemplatesWithVariables() {
        NotificationTemplate emailTemplate = NotificationTemplate.builder()
                .eventType("SALE_CONFIRMED")
                .channel(NotificationChannel.EMAIL)
                .subject("Hola {clientName}")
                .body("Factura {invoiceNumber} total {total} saldo {balance} {pdfUrl}")
                .enabled(true)
                .build();
        templateRepository.save(emailTemplate);

        NotificationTemplate whatsappTemplate = NotificationTemplate.builder()
                .eventType("SALE_CONFIRMED")
                .channel(NotificationChannel.WHATSAPP)
                .body("Licencias: {licenses}")
                .enabled(true)
                .build();
        templateRepository.save(whatsappTemplate);

        Map<String, String> vars = Map.of(
                "clientName", "Ana Perez",
                "invoiceNumber", "INV-0001",
                "total", "120.00",
                "balance", "0",
                "pdfUrl", "https://files.example.com/inv.pdf",
                "licenses", "ABC-1, ABC-2",
                "dueDate", "2026-01-10"
        );

        NotificationTemplateService.ResolvedTemplate resolved =
                templateService.resolveTemplates("SALE_CONFIRMED", vars);

        assertThat(resolved.emailSubject()).isEqualTo("Hola Ana Perez");
        assertThat(resolved.emailBody()).contains("INV-0001")
                .contains("120.00")
                .contains("https://files.example.com/inv.pdf");
        assertThat(resolved.whatsappText()).isEqualTo("Licencias: ABC-1, ABC-2");
    }
}
