package com.dismal.distribuciones.modules.integrations.crm.service;

import com.dismal.distribuciones.modules.integrations.crm.event.SaleConfirmedWhatsappEvent;
import com.dismal.distribuciones.modules.integrations.notifications.domain.NotificationChannel;
import com.dismal.distribuciones.modules.integrations.notifications.service.NotificationTemplateService;
import com.dismal.distribuciones.modules.integrations.outbox.service.OutboxService;
import com.dismal.distribuciones.modules.sales.domain.AccountsReceivable;
import com.dismal.distribuciones.modules.sales.domain.Sale;
import com.dismal.distribuciones.modules.sales.service.SaleNumberService;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.store.domain.License;
import com.dismal.distribuciones.modules.store.domain.Software;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleWhatsappPublisherService {

    private static final String COMPANY_NAME = "Dismal";
    private static final String SUPPORT_EMAIL = "soporte@dismal.com";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ApplicationEventPublisher eventPublisher;
    private final NotificationTemplateService templateService;

    public void publishSaleConfirmed(Sale sale, AccountsReceivable accountsReceivable, List<License> licenses,
                                     List<NotificationChannel> deliveryChannels, UUID outboxEventId) {
        if (!shouldSendWhatsapp(deliveryChannels)) {
            return;
        }

        User client = sale.getClient();
        if (client == null) {
            return;
        }

        String message = buildMessage(sale, client, accountsReceivable, licenses);
        eventPublisher.publishEvent(new SaleConfirmedWhatsappEvent(this, client.getPhone(), message, outboxEventId));
    }

    private boolean shouldSendWhatsapp(List<NotificationChannel> deliveryChannels) {
        return deliveryChannels == null || deliveryChannels.isEmpty()
                || deliveryChannels.contains(NotificationChannel.WHATSAPP);
    }

    private String buildMessage(Sale sale, User client, AccountsReceivable accountsReceivable, List<License> licenses) {
        String eventType = licenses != null && !licenses.isEmpty()
                ? OutboxService.EVENT_LICENSES_DELIVERED
                : OutboxService.EVENT_SALE_CONFIRMED;
        Map<String, String> vars = buildVariables(sale, client, accountsReceivable, licenses);
        NotificationTemplateService.ResolvedTemplate template = templateService.resolveTemplates(eventType, vars);
        if (template.whatsappText() != null && !template.whatsappText().isBlank()) {
            return template.whatsappText();
        }

        String customerName = buildCustomerName(client);
        StringBuilder message = new StringBuilder();
        message.append("Hola ").append(customerName).append(", tu compra en ").append(COMPANY_NAME).append(" fue confirmada.");
        message.append("\n\n");
        message.append("Venta: ").append(publicSaleReference(sale));
        message.append("\n");
        message.append("Total: ").append(formatMoney(sale.getTotal()));

        if (accountsReceivable != null) {
            message.append("\n");
            message.append("Saldo pendiente: ").append(formatMoney(accountsReceivable.getBalance()));
            message.append("\n");
            message.append("Vence: ").append(accountsReceivable.getDueDate().format(DATE_FORMATTER));
        }

        if (licenses != null && !licenses.isEmpty()) {
            message.append("\n\n");
            message.append("Productos y seriales:");
            message.append("\n");
            message.append(formatLicenses(licenses));
        }

        message.append("\n\n");
        message.append("Guarda este mensaje para futuras activaciones. Soporte: ").append(SUPPORT_EMAIL);
        return message.toString();
    }

    private Map<String, String> buildVariables(
            Sale sale,
            User client,
            AccountsReceivable accountsReceivable,
            List<License> licenses
    ) {
        Map<String, String> vars = new HashMap<>();
        vars.put("clientName", buildCustomerName(client));
        String saleUuid = sale.getId() != null ? sale.getId().toString() : "";
        String publicReference = publicSaleReference(sale);
        vars.put("saleId", publicReference);
        vars.put("saleNumber", publicReference);
        vars.put("saleUuid", saleUuid);
        vars.put("saleType", sale.getSaleType() != null ? sale.getSaleType().name() : "");
        vars.put("companyName", COMPANY_NAME);
        vars.put("supportEmail", SUPPORT_EMAIL);
        vars.put("paymentStatus", accountsReceivable != null && accountsReceivable.getBalance() != null
                && accountsReceivable.getBalance().signum() > 0 ? "Saldo pendiente" : "Pagado");
        vars.put("total", sale.getTotal() != null ? sale.getTotal().toPlainString() : "0");
        vars.put("balance", accountsReceivable != null && accountsReceivable.getBalance() != null
                ? accountsReceivable.getBalance().toPlainString()
                : "0");
        vars.put("dueDate", accountsReceivable != null && accountsReceivable.getDueDate() != null
                ? accountsReceivable.getDueDate().format(DATE_FORMATTER)
                : "");
        vars.put("licenses", formatLicenses(licenses));
        vars.put("items", formatItems(sale));
        return vars;
    }

    private String publicSaleReference(Sale sale) {
        String saleNumber = SaleNumberService.format(sale.getCountry(), sale.getSaleNumber());
        return saleNumber != null
                ? saleNumber
                : sale.getId() != null ? sale.getId().toString() : "";
    }

    private String formatLicenses(List<License> licenses) {
        if (licenses == null || licenses.isEmpty()) {
            return "";
        }
        Map<LicenseDeliveryLine, Integer> grouped = new LinkedHashMap<>();
        for (License license : licenses) {
            Software software = license.getSoftware();
            String name = software != null && software.getName() != null ? software.getName() : "Producto";
            String description = software != null && software.getDescription() != null ? software.getDescription().trim() : "";
            String serial = license.getLicenseKey() != null ? license.getLicenseKey() : "";
            LicenseDeliveryLine line = new LicenseDeliveryLine(name, description, serial);
            grouped.put(line, grouped.getOrDefault(line, 0) + 1);
        }
        return grouped.entrySet().stream()
                .map(entry -> {
                    LicenseDeliveryLine line = entry.getKey();
                    String activations = entry.getValue() > 1
                            ? entry.getValue() + " dispositivos / activaciones"
                            : "1 dispositivo / activacion";
                    StringBuilder builder = new StringBuilder();
                    builder.append("- ").append(line.productName()).append("\n");
                    if (!line.description().isBlank()) {
                        builder.append("  ").append(line.description()).append("\n");
                    }
                    builder.append("  Serial: ").append(line.serial()).append("\n");
                    builder.append("  Uso incluido: ").append(activations);
                    return builder.toString();
                })
                .collect(Collectors.joining("\n\n"));
    }

    private String formatItems(Sale sale) {
        if (sale.getItems() == null || sale.getItems().isEmpty()) {
            return "";
        }
        return sale.getItems().stream()
                .map(item -> {
                    String name = item.getSoftware() != null ? item.getSoftware().getName() : "Producto";
                    int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
                    return "- " + name + " x" + quantity;
                })
                .collect(Collectors.joining("\n"));
    }

    private String buildCustomerName(User client) {
        String firstname = client.getFirstname() != null ? client.getFirstname().trim() : "";
        String lastname = client.getLastname() != null ? client.getLastname().trim() : "";
        String fullName = (firstname + " " + lastname).trim();
        return fullName.isBlank() ? client.getEmail() : fullName;
    }

    private String formatMoney(BigDecimal amount) {
        return amount != null ? amount.toPlainString() + " USD" : "0 USD";
    }

    private record LicenseDeliveryLine(String productName, String description, String serial) {}
}
