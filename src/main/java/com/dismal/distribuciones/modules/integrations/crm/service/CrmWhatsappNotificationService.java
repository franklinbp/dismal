package com.dismal.distribuciones.modules.integrations.crm.service;

import com.dismal.distribuciones.modules.integrations.notifications.domain.NotificationChannel;
import com.dismal.distribuciones.modules.integrations.notifications.dto.NotificationSendResult;
import com.dismal.distribuciones.modules.integrations.notifications.service.NotificationDeliveryLogService;
import com.dismal.distribuciones.modules.integrations.notifications.service.NotificationTemplateService;
import com.dismal.distribuciones.modules.security.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrmWhatsappNotificationService {

    private static final String COMPANY_NAME = "Dismal";
    private static final String SUPPORT_EMAIL = "soporte@dismal.com";

    private final DismalCrmGateway gateway;
    private final NotificationTemplateService templateService;
    private final NotificationDeliveryLogService deliveryLogService;
    private final CrmWhatsappLineResolverService lineResolverService;

    @Value("${integrations.dismal-crm.whatsapp-enabled:false}")
    private boolean whatsappEnabled;

    public NotificationSendResult sendMessage(String phone, String message, UUID outboxEventId) {
        if (!lineResolverService.isWhatsappEnabled(whatsappEnabled)) {
            return fail(outboxEventId, "DismalCRM WhatsApp integration is disabled.");
        }
        if (phone == null || phone.isBlank()) {
            return fail(outboxEventId, "Client phone is empty.");
        }
        if (message == null || message.isBlank()) {
            return fail(outboxEventId, "WhatsApp message is empty.");
        }

        try {
            gateway.sendWhatsAppMessage(new DismalCrmGateway.SendWhatsAppMessageRequest(
                    phone,
                    message,
                    lineResolverService.resolveWhatsappId(phone)
            ));
            if (outboxEventId != null) {
                deliveryLogService.markSent(outboxEventId, NotificationChannel.WHATSAPP, "DismalCRM");
            }
            return new NotificationSendResult(true, "WhatsApp sent to " + phone + ".");
        } catch (Exception ex) {
            String detail = ex.getMessage() != null ? ex.getMessage() : "Unknown DismalCRM WhatsApp error.";
            log.warn("DismalCRM WhatsApp request failed phone={} error={}", maskPhone(phone), detail, ex);
            return fail(outboxEventId, detail);
        }
    }

    public NotificationSendResult sendArReminder(User client, BigDecimal balance, long daysOverdue,
                                                 LocalDate dueDate, UUID outboxEventId) {
        if (client == null) {
            return fail(outboxEventId, "Client is missing.");
        }
        Map<String, String> vars = buildArVariables(client, balance, daysOverdue, dueDate);
        NotificationTemplateService.ResolvedTemplate template =
                templateService.resolveTemplates("AR_OVERDUE", vars);
        String message = template.whatsappText() != null && !template.whatsappText().isBlank()
                ? template.whatsappText()
                : defaultArReminderMessage(vars);
        return sendMessage(client.getPhone(), message, outboxEventId);
    }

    private NotificationSendResult fail(UUID outboxEventId, String message) {
        if (outboxEventId != null) {
            deliveryLogService.markFailed(outboxEventId, NotificationChannel.WHATSAPP, message);
        }
        log.warn("WhatsApp delivery failed: {}", message);
        return new NotificationSendResult(false, message);
    }

    private Map<String, String> buildArVariables(User client, BigDecimal balance, long daysOverdue, LocalDate dueDate) {
        Map<String, String> vars = new HashMap<>();
        vars.put("clientName", safeName(client));
        vars.put("balance", formatAmount(balance));
        vars.put("daysOverdue", String.valueOf(Math.max(daysOverdue, 0)));
        vars.put("dueDate", dueDate != null ? dueDate.toString() : "");
        vars.put("companyName", COMPANY_NAME);
        vars.put("supportEmail", SUPPORT_EMAIL);
        return vars;
    }

    private String defaultArReminderMessage(Map<String, String> vars) {
        StringBuilder builder = new StringBuilder();
        builder.append("Hola ").append(vars.getOrDefault("clientName", "cliente")).append(", ");
        builder.append("te recordamos que tienes un saldo pendiente de $")
                .append(vars.getOrDefault("balance", "0.00"))
                .append(" USD en ").append(COMPANY_NAME).append(".");
        String dueDate = vars.getOrDefault("dueDate", "");
        if (!dueDate.isBlank()) {
            builder.append("\nVence: ").append(dueDate);
        }
        String daysOverdue = vars.getOrDefault("daysOverdue", "0");
        if (!"0".equals(daysOverdue)) {
            builder.append("\nDias vencidos: ").append(daysOverdue);
        }
        builder.append("\nSi ya realizaste el pago, comparte el comprobante. Soporte: ").append(SUPPORT_EMAIL);
        return builder.toString();
    }

    private String safeName(User client) {
        String first = client.getFirstname() != null ? client.getFirstname() : "";
        String last = client.getLastname() != null ? client.getLastname() : "";
        String full = (first + " " + last).trim();
        return full.isBlank() ? client.getEmail() : full;
    }

    private String formatAmount(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "empty";
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() <= 4) {
            return "****";
        }
        return "*".repeat(digits.length() - 4) + digits.substring(digits.length() - 4);
    }
}
