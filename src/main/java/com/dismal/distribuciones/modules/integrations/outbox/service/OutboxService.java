package com.dismal.distribuciones.modules.integrations.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dismal.distribuciones.modules.integrations.outbox.domain.EventOutbox;
import com.dismal.distribuciones.modules.integrations.outbox.domain.EventOutboxStatus;
import com.dismal.distribuciones.modules.integrations.outbox.repository.EventOutboxRepository;
import com.dismal.distribuciones.modules.integrations.notifications.domain.NotificationChannel;
import com.dismal.distribuciones.modules.integrations.notifications.service.NotificationTemplateService;
import com.dismal.distribuciones.modules.sales.domain.Invoice;
import com.dismal.distribuciones.modules.sales.domain.Payment;
import com.dismal.distribuciones.modules.sales.domain.Sale;
import com.dismal.distribuciones.modules.sales.service.SaleNumberService;
import com.dismal.distribuciones.modules.sales.domain.SaleItem;
import com.dismal.distribuciones.modules.sales.domain.SaleType;
import com.dismal.distribuciones.modules.sales.repository.InvoiceRepository;
import com.dismal.distribuciones.modules.marketing.domain.Campaign;
import com.dismal.distribuciones.modules.marketing.domain.CampaignChannel;
import com.dismal.distribuciones.modules.store.domain.License;
import com.dismal.distribuciones.modules.store.domain.Software;
import com.dismal.distribuciones.modules.security.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private static final String COMPANY_NAME = "Dismal";
    private static final String SUPPORT_EMAIL = "soporte@dismal.com";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final String EVENT_SALE_CONFIRMED = "SALE_CONFIRMED";
    public static final String EVENT_INVOICE_ISSUED = "INVOICE_ISSUED";
    public static final String EVENT_PAYMENT_RECEIVED = "PAYMENT_RECEIVED";
    public static final String EVENT_LICENSES_DELIVERED = "LICENSES_DELIVERED";
    public static final String EVENT_AR_OVERDUE = "AR_OVERDUE";
    public static final String EVENT_SALE_CANCELLED = "SALE_CANCELLED";
    public static final String EVENT_CAMPAIGN_SCHEDULED = "CAMPAIGN_SCHEDULED";
    public static final String EVENT_CAMPAIGN_SENT = "CAMPAIGN_SENT";

    private final EventOutboxRepository eventOutboxRepository;
    private final NotificationTemplateService notificationTemplateService;
    private final InvoiceRepository invoiceRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.invoice.pdf-base-url:}")
    private String invoicePdfBaseUrl;

    public void enqueueSaleConfirmedEvent(Sale sale, LocalDate dueDate, BigDecimal balance) {
        UUID idempotencyKey = UUID.randomUUID();
        String payloadJson = buildSalePayload(sale, EVENT_SALE_CONFIRMED, idempotencyKey, dueDate, balance, null, null, null, null);

        EventOutbox outbox = EventOutbox.builder()
                .id(idempotencyKey)
                .eventType(EVENT_SALE_CONFIRMED)
                .aggregateId(sale.getId())
                .payloadJson(payloadJson)
                .status(EventOutboxStatus.PENDING)
                .attempts(0)
                .build();
        eventOutboxRepository.save(outbox);
    }

    public void enqueuePaymentReceivedEvent(Sale sale, Payment payment, LocalDate dueDate, BigDecimal balance) {
        UUID idempotencyKey = UUID.randomUUID();
        String payloadJson = buildSalePayload(sale, EVENT_PAYMENT_RECEIVED, idempotencyKey, dueDate, balance, payment, null, null, null);

        EventOutbox outbox = EventOutbox.builder()
                .id(idempotencyKey)
                .eventType(EVENT_PAYMENT_RECEIVED)
                .aggregateId(sale.getId())
                .payloadJson(payloadJson)
                .status(EventOutboxStatus.PENDING)
                .attempts(0)
                .build();
        eventOutboxRepository.save(outbox);
    }

    public UUID enqueueLicenseDeliveredEvent(Sale sale, List<License> deliveredLicenses, LocalDate dueDate, BigDecimal balance,
                                             List<NotificationChannel> deliveryChannels) {
        if (deliveredLicenses == null || deliveredLicenses.isEmpty()) {
            return null;
        }
        UUID idempotencyKey = UUID.randomUUID();
        String payloadJson = buildSalePayload(sale, EVENT_LICENSES_DELIVERED, idempotencyKey, dueDate, balance, null, deliveredLicenses, null, deliveryChannels);

        EventOutbox outbox = EventOutbox.builder()
                .id(idempotencyKey)
                .eventType(EVENT_LICENSES_DELIVERED)
                .aggregateId(sale.getId())
                .payloadJson(payloadJson)
                .status(EventOutboxStatus.PENDING)
                .attempts(0)
                .build();
        eventOutboxRepository.save(outbox);
        return idempotencyKey;
    }

    public void enqueueInvoiceIssuedEvent(Invoice invoice, Sale sale, LocalDate dueDate, BigDecimal balance) {
        UUID idempotencyKey = UUID.randomUUID();
        String payloadJson = buildSalePayload(sale, EVENT_INVOICE_ISSUED, idempotencyKey, dueDate, balance, null, null, invoice, null);

        EventOutbox outbox = EventOutbox.builder()
                .id(idempotencyKey)
                .eventType(EVENT_INVOICE_ISSUED)
                .aggregateId(sale.getId())
                .payloadJson(payloadJson)
                .status(EventOutboxStatus.PENDING)
                .attempts(0)
                .build();
        eventOutboxRepository.save(outbox);
    }

    public void enqueueSaleCancelledEvent(Sale sale, LocalDate dueDate, BigDecimal balance) {
        UUID idempotencyKey = UUID.randomUUID();
        String payloadJson = buildSalePayload(sale, EVENT_SALE_CANCELLED, idempotencyKey, dueDate, balance, null, null, null, null);

        EventOutbox outbox = EventOutbox.builder()
                .id(idempotencyKey)
                .eventType(EVENT_SALE_CANCELLED)
                .aggregateId(sale.getId())
                .payloadJson(payloadJson)
                .status(EventOutboxStatus.PENDING)
                .attempts(0)
                .build();
        eventOutboxRepository.save(outbox);
    }

    public UUID enqueueArOverdueEvent(User client, BigDecimal balance, long daysOverdue, LocalDate dueDate, UUID idempotencyKey) {
        if (eventOutboxRepository.existsById(idempotencyKey)) {
            return idempotencyKey;
        }
        String payloadJson = buildArOverduePayload(client, balance, daysOverdue, dueDate, idempotencyKey);
        EventOutbox outbox = EventOutbox.builder()
                .id(idempotencyKey)
                .eventType(EVENT_AR_OVERDUE)
                .aggregateId(client.getId())
                .payloadJson(payloadJson)
                .status(EventOutboxStatus.PENDING)
                .attempts(0)
                .build();
        eventOutboxRepository.save(outbox);
        return idempotencyKey;
    }

    public UUID enqueueTestEvent(String to, String message) {
        UUID idempotencyKey = UUID.randomUUID();
        String payloadJson = buildTestPayload(idempotencyKey, to, message);

        EventOutbox outbox = EventOutbox.builder()
                .id(idempotencyKey)
                .eventType("integration.test")
                .aggregateId(idempotencyKey)
                .payloadJson(payloadJson)
                .status(EventOutboxStatus.PENDING)
                .attempts(0)
                .build();
        eventOutboxRepository.save(outbox);
        return idempotencyKey;
    }

    public void enqueueCampaignScheduledEvent(Campaign campaign, CampaignChannel channel, List<User> recipients) {
        enqueueCampaignEvent(campaign, channel, recipients, EVENT_CAMPAIGN_SCHEDULED);
    }

    public void enqueueCampaignSentEvent(Campaign campaign, CampaignChannel channel, List<User> recipients) {
        enqueueCampaignEvent(campaign, channel, recipients, EVENT_CAMPAIGN_SENT);
    }

    private void enqueueCampaignEvent(Campaign campaign, CampaignChannel channel, List<User> recipients, String eventType) {
        UUID idempotencyKey = UUID.randomUUID();
        String payloadJson = buildCampaignPayload(campaign, channel, recipients, idempotencyKey, eventType);

        EventOutbox outbox = EventOutbox.builder()
                .id(idempotencyKey)
                .eventType(eventType)
                .aggregateId(campaign.getId())
                .payloadJson(payloadJson)
                .status(EventOutboxStatus.PENDING)
                .attempts(0)
                .build();
        eventOutboxRepository.save(outbox);
    }

    private String buildSalePayload(Sale sale, String eventType, UUID idempotencyKey,
                                    LocalDate dueDate, BigDecimal balance, Payment payment,
                                    List<License> deliveredLicenses, Invoice invoiceOverride,
                                    List<NotificationChannel> deliveryChannels) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", eventType);
        payload.put("saleId", sale.getId());
        payload.put("saleNumber", SaleNumberService.format(sale.getCountry(), sale.getSaleNumber()));
        payload.put("saleType", sale.getSaleType());
        payload.put("paymentType", sale.getSaleType());
        payload.put("status", sale.getStatus());
        payload.put("total", sale.getTotal());
        payload.put("currency", sale.getCurrency());

        payload.put("client", buildClientPayload(sale.getClient()));
        payload.put("items", buildItemPayloads(sale.getItems()));

        Map<String, Object> saleSummary = new LinkedHashMap<>();
        saleSummary.put("id", sale.getId());
        saleSummary.put("number", SaleNumberService.format(sale.getCountry(), sale.getSaleNumber()));
        saleSummary.put("saleType", sale.getSaleType());
        saleSummary.put("status", sale.getStatus());
        saleSummary.put("total", sale.getTotal());
        saleSummary.put("currency", sale.getCurrency());
        payload.put("sale", saleSummary);

        Map<String, Object> ar = new LinkedHashMap<>();
        if (sale.getSaleType() == SaleType.CREDIT) {
            ar.put("dueDate", dueDate);
            ar.put("balance", balance != null ? balance : sale.getTotal());
        } else {
            ar.put("dueDate", null);
            ar.put("balance", BigDecimal.ZERO);
        }
        payload.put("ar", ar);

        if (payment != null) {
            Map<String, Object> paymentPayload = new LinkedHashMap<>();
            paymentPayload.put("paymentId", payment.getId());
            paymentPayload.put("amount", payment.getAmount());
            paymentPayload.put("method", payment.getMethod());
            paymentPayload.put("reference", payment.getReference());
            paymentPayload.put("createdAt", payment.getCreatedAt());
            payload.put("payment", paymentPayload);
        }

        if (deliveredLicenses != null && !deliveredLicenses.isEmpty()) {
            List<String> licenseKeys = deliveredLicenses.stream()
                    .map(License::getLicenseKey)
                    .toList();
            payload.put("licenses", licenseKeys);
            payload.put("licensesDelivered", licenseKeys);
            payload.put("licensesDetailed", buildLicensePayloads(deliveredLicenses));
        }

        Invoice invoice = invoiceOverride != null
                ? invoiceOverride
                : invoiceRepository.findBySaleId(sale.getId()).orElse(null);
        if (invoice != null) {
            Map<String, Object> invoicePayload = new LinkedHashMap<>();
            invoicePayload.put("number", invoice.getInvoiceNumber());
            invoicePayload.put("pdfUrl", buildInvoicePdfUrl(invoice));
            payload.put("invoice", invoicePayload);
        } else {
            payload.put("invoice", null);
        }

        Map<String, String> variables = buildTemplateVariables(sale, dueDate, balance, deliveredLicenses, invoice);
        NotificationTemplateService.ResolvedTemplate resolvedTemplate =
                notificationTemplateService.resolveTemplates(eventType, variables);
        Map<String, Object> templateResolved = new LinkedHashMap<>();
        boolean allowEmail = allowsChannel(deliveryChannels, NotificationChannel.EMAIL);
        boolean allowWhatsapp = allowsChannel(deliveryChannels, NotificationChannel.WHATSAPP);
        templateResolved.put("emailSubject", allowEmail ? resolvedTemplate.emailSubject() : null);
        templateResolved.put("emailBody", allowEmail ? resolvedTemplate.emailBody() : null);
        templateResolved.put("whatsappText", allowWhatsapp ? resolvedTemplate.whatsappText() : null);
        payload.put("templateResolved", templateResolved);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("timestamp", LocalDateTime.now());
        meta.put("idempotencyKey", idempotencyKey);
        payload.put("meta", meta);

        if (deliveryChannels != null && !deliveryChannels.isEmpty()) {
            payload.put("deliveryChannels", deliveryChannels);
        }

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }

    private String buildArOverduePayload(User client, BigDecimal balance, long daysOverdue, LocalDate dueDate,
                                         UUID idempotencyKey) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", EVENT_AR_OVERDUE);
        payload.put("client", buildClientPayload(client));
        Map<String, Object> ar = new LinkedHashMap<>();
        ar.put("balance", balance);
        ar.put("daysOverdue", daysOverdue);
        ar.put("dueDate", dueDate);
        payload.put("ar", ar);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("timestamp", LocalDateTime.now());
        meta.put("idempotencyKey", idempotencyKey);
        payload.put("meta", meta);

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }

    private Map<String, String> buildTemplateVariables(Sale sale, LocalDate dueDate, BigDecimal balance,
                                                       List<License> deliveredLicenses, Invoice invoice) {
        Map<String, String> vars = new HashMap<>();
        vars.put("clientName", resolveClientName(sale.getClient()));
        String saleUuid = sale.getId() != null ? sale.getId().toString() : "";
        String saleNumber = SaleNumberService.format(sale.getCountry(), sale.getSaleNumber());
        String publicReference = saleNumber != null ? saleNumber : saleUuid;
        vars.put("saleId", publicReference);
        vars.put("saleNumber", publicReference);
        vars.put("saleUuid", saleUuid);
        vars.put("saleType", sale.getSaleType() != null ? sale.getSaleType().name() : "");
        vars.put("saleDate", sale.getCreatedAt() != null ? sale.getCreatedAt().format(DATE_FORMATTER) : LocalDate.now().format(DATE_FORMATTER));
        vars.put("items", formatSaleItems(sale));
        vars.put("invoiceNumber", invoice != null ? invoice.getInvoiceNumber() : "");
        vars.put("total", formatAmount(sale.getTotal()));
        vars.put("balance", formatAmount(balance));
        vars.put("dueDate", dueDate != null ? dueDate.toString() : "");
        vars.put("paymentStatus", balance != null && balance.signum() > 0 ? "Saldo pendiente" : "Pagado");
        vars.put("licenses", formatLicenseSummary(deliveredLicenses));
        vars.put("pdfUrl", invoice != null ? buildInvoicePdfUrl(invoice) : "");
        vars.put("companyName", COMPANY_NAME);
        vars.put("supportEmail", SUPPORT_EMAIL);
        return vars;
    }

    private String buildInvoicePdfUrl(Invoice invoice) {
        if (invoice == null || invoicePdfBaseUrl == null || invoicePdfBaseUrl.isBlank()) {
            return null;
        }
        return invoicePdfBaseUrl.endsWith("/")
                ? invoicePdfBaseUrl + invoice.getId()
                : invoicePdfBaseUrl + "/" + invoice.getId();
    }

    private String resolveClientName(User user) {
        if (user == null) {
            return "";
        }
        String name = (user.getFirstname() != null ? user.getFirstname() : "") +
                (user.getLastname() != null ? " " + user.getLastname() : "");
        return name.trim();
    }

    private String buildTestPayload(UUID idempotencyKey, String to, String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", "integration.test");
        payload.put("to", to);
        payload.put("message", message != null ? message : "hello");

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("timestamp", LocalDateTime.now());
        meta.put("idempotencyKey", idempotencyKey);
        payload.put("meta", meta);

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }

    private String buildCampaignPayload(Campaign campaign, CampaignChannel channel, List<User> recipients,
                                        UUID idempotencyKey, String eventType) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", eventType);
        payload.put("campaignId", campaign.getId());
        payload.put("title", campaign.getTitle());
        payload.put("channel", channel);
        payload.put("message", campaign.getMessageBody());
        payload.put("imageUrl", campaign.getImageUrl());

        Software product = campaign.getProduct();
        if (product != null) {
            Map<String, Object> productPayload = new LinkedHashMap<>();
            productPayload.put("id", product.getId());
            productPayload.put("name", product.getName());
            payload.put("product", productPayload);
        } else {
            payload.put("product", null);
        }

        List<Map<String, Object>> clients = recipients.stream()
                .map(this::buildCampaignClientPayload)
                .toList();
        payload.put("clients", clients);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("timestamp", LocalDateTime.now());
        meta.put("idempotencyKey", idempotencyKey);
        payload.put("meta", meta);

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }

    private Map<String, Object> buildClientPayload(User user) {
        Map<String, Object> client = new LinkedHashMap<>();
        if (user == null) {
            client.put("id", null);
            client.put("email", null);
            client.put("phone", null);
            client.put("name", "");
            return client;
        }
        client.put("id", user.getId());
        client.put("email", user.getEmail());
        client.put("phone", user.getPhone());
        String name = (user.getFirstname() != null ? user.getFirstname() : "") +
                (user.getLastname() != null ? " " + user.getLastname() : "");
        client.put("name", name.trim());
        return client;
    }

    private List<Map<String, Object>> buildItemPayloads(List<SaleItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    Software software = item.getSoftware();
                    map.put("softwareId", software != null ? software.getId() : null);
                    map.put("name", software != null ? software.getName() : "Producto");
                    map.put("qty", item.getQuantity());
                    map.put("unitPrice", item.getUnitPrice());
                    return map;
                })
                .toList();
    }

    private List<Map<String, Object>> buildLicensePayloads(List<License> licenses) {
        return licenses.stream()
                .map(license -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    Software software = license.getSoftware();
                    map.put("licenseId", license.getId());
                    map.put("licenseKey", license.getLicenseKey());
                    map.put("maxActivations", license.getMaxActivations());
                    map.put("usedActivations", license.getUsedActivations());
                    if (software != null) {
                        map.put("softwareId", software.getId());
                        map.put("softwareName", software.getName());
                        map.put("softwareDescription", software.getDescription());
                    }
                    return map;
                })
                .toList();
    }

    private String formatLicenseSummary(List<License> licenses) {
        if (licenses == null || licenses.isEmpty()) {
            return "";
        }
        Map<LicenseDeliveryLine, Integer> grouped = new LinkedHashMap<>();
        for (License license : licenses) {
            Software software = license.getSoftware();
            String name = software != null && software.getName() != null ? software.getName() : "Producto";
            String description = software != null && software.getDescription() != null ? software.getDescription().trim() : "";
            String key = license.getLicenseKey() != null ? license.getLicenseKey() : "";
            LicenseDeliveryLine line = new LicenseDeliveryLine(name, description, key);
            grouped.put(line, grouped.getOrDefault(line, 0) + 1);
        }
        return grouped.entrySet().stream()
                .map(entry -> {
                    LicenseDeliveryLine line = entry.getKey();
                    String activations = entry.getValue() > 1
                            ? " (" + entry.getValue() + " dispositivos / activaciones)"
                            : " (1 dispositivo / activacion)";
                    StringBuilder builder = new StringBuilder();
                    builder.append("- ").append(line.productName()).append(activations).append("\n");
                    if (!line.description().isBlank()) {
                        builder.append("  Descripcion: ").append(line.description()).append("\n");
                    }
                    builder.append("  Serial: ").append(line.serial());
                    return builder.toString();
                })
                .collect(Collectors.joining("\n"));
    }

    private String formatSaleItems(Sale sale) {
        if (sale.getItems() == null || sale.getItems().isEmpty()) {
            return "";
        }
        return sale.getItems().stream()
                .map(item -> {
                    String name = item.getSoftware() != null ? item.getSoftware().getName() : "Producto";
                    int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
                    String unitPrice = formatAmount(item.getUnitPrice());
                    return "- " + name + " x" + quantity + " | Unitario: $" + unitPrice;
                })
                .collect(Collectors.joining("\n"));
    }

    private String formatAmount(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private boolean allowsChannel(List<NotificationChannel> channels, NotificationChannel channel) {
        if (channels == null || channels.isEmpty()) {
            return true;
        }
        return channels.contains(channel);
    }

    private Map<String, Object> buildCampaignClientPayload(User user) {
        Map<String, Object> client = new LinkedHashMap<>();
        client.put("id", user.getId());
        client.put("email", user.getEmail());
        client.put("phone", user.getPhone());
        String name = (user.getFirstname() != null ? user.getFirstname() : "") +
                (user.getLastname() != null ? " " + user.getLastname() : "");
        client.put("name", name.trim());
        return client;
    }

    private record LicenseDeliveryLine(String productName, String description, String serial) {}
}
