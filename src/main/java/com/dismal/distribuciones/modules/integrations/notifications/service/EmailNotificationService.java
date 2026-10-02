package com.dismal.distribuciones.modules.integrations.notifications.service;

import com.dismal.distribuciones.modules.integrations.settings.domain.IntegrationSettings;
import com.dismal.distribuciones.modules.integrations.settings.service.IntegrationSettingsService;
import com.dismal.distribuciones.modules.integrations.notifications.dto.NotificationSendResult;
import com.dismal.distribuciones.modules.sales.domain.AccountsReceivable;
import com.dismal.distribuciones.modules.sales.domain.Sale;
import com.dismal.distribuciones.modules.sales.domain.Invoice;
import com.dismal.distribuciones.modules.sales.repository.InvoiceRepository;
import com.dismal.distribuciones.modules.sales.service.SaleNumberService;
import com.dismal.distribuciones.exception.BadRequestException;
import com.dismal.distribuciones.modules.store.domain.License;
import com.dismal.distribuciones.modules.store.domain.Software;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private static final String COMPANY_NAME = "Dismal";
    private static final String SUPPORT_EMAIL = "soporte@dismal.com";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final IntegrationSettingsService settingsService;
    private final NotificationTemplateService templateService;
    private final InvoiceRepository invoiceRepository;

    public void sendSaleConfirmed(Sale sale, AccountsReceivable ar) {
        if (sale == null || sale.getClient() == null) {
            return;
        }
        IntegrationSettings settings = settingsService.getSettingsEntity();
        if (settings == null || !settings.isSmtpEnabled()) {
            return;
        }
        String to = sale.getClient().getEmail();
        if (to == null || to.isBlank()) {
            return;
        }

        JavaMailSenderImpl sender = buildSender(settings);
        if (sender == null) {
            return;
        }

        Map<String, String> vars = new HashMap<>();
        vars.put("clientName", safeName(sale));
        putSaleIdentity(vars, sale);
        vars.put("saleType", sale.getSaleType() != null ? sale.getSaleType().name() : "");
        vars.put("saleDate", sale.getCreatedAt() != null ? sale.getCreatedAt().format(DATE_FORMATTER) : LocalDate.now().format(DATE_FORMATTER));
        vars.put("items", formatSaleItems(sale));
        vars.put("total", formatAmount(sale.getTotal()));
        vars.put("balance", formatAmount(ar != null ? ar.getBalance() : BigDecimal.ZERO));
        vars.put("dueDate", ar != null && ar.getDueDate() != null ? ar.getDueDate().toString() : "");
        vars.put("paymentStatus", ar != null && ar.getBalance() != null && ar.getBalance().signum() > 0 ? "Saldo pendiente" : "Pagado");
        vars.put("companyName", COMPANY_NAME);
        vars.put("supportEmail", SUPPORT_EMAIL);

        NotificationTemplateService.ResolvedTemplate template =
                templateService.resolveTemplates("SALE_CONFIRMED", vars);

        String subject = template.emailSubject() != null ? template.emailSubject() : "Venta confirmada";
        String body = template.emailBody() != null
                ? template.emailBody()
                : defaultBody(vars, ar != null ? ar.getDueDate() : null);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        message.setFrom(resolveFrom(settings));
        if (settings.getSmtpUser() != null && !settings.getSmtpUser().isBlank()) {
            message.setReplyTo(settings.getSmtpUser());
        }

        try {
            sender.send(message);
        } catch (Exception ex) {
            log.warn("Failed to send sale confirmation email to {}", to, ex);
        }
    }

    public void sendTestEmail(String to, String subject, String body) {
        sendTestEmail(to, subject, body, settingsService.getSettingsEntity());
    }

    public NotificationSendResult sendAccountEmail(String to, String subject, String body, String htmlBody) {
        IntegrationSettings settings = settingsService.getSettingsEntity();
        if (settings == null || !settings.isSmtpEnabled()) {
            return new NotificationSendResult(false, "SMTP is disabled.");
        }
        if (to == null || to.isBlank()) {
            return new NotificationSendResult(false, "Recipient is empty.");
        }
        JavaMailSenderImpl sender = buildSender(settings);
        if (sender == null) {
            return new NotificationSendResult(false, "SMTP settings are incomplete.");
        }
        try {
            sendEmail(sender, settings, to.trim(), subject, body, htmlBody);
            return new NotificationSendResult(true, "Email sent.");
        } catch (Exception ex) {
            log.warn("Failed to send account email to {}", to, ex);
            return new NotificationSendResult(false,
                    ex.getMessage() != null ? ex.getMessage() : "Unknown SMTP error.");
        }
    }

    public void sendTestEmail(String to, String subject, String body, IntegrationSettings settings) {
        if (settings == null || !settings.isSmtpEnabled()) {
            throw new BadRequestException("SMTP is disabled.");
        }

        JavaMailSenderImpl sender = buildSender(settings);
        if (sender == null) {
            throw new BadRequestException("SMTP settings are incomplete.");
        }

        String resolvedTo = to != null && !to.isBlank() ? to.trim() : resolveDefaultRecipient(settings);
        if (resolvedTo == null || resolvedTo.isBlank()) {
            throw new BadRequestException("Test email recipient is required.");
        }

        String resolvedSubject = subject != null && !subject.isBlank()
                ? subject.trim()
                : "Test SMTP - Dismal";
        String resolvedBody = body != null && !body.isBlank()
                ? body.trim()
                : "Este es un email de prueba enviado desde Dismal.\nFecha: " + LocalDateTime.now();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(resolvedTo);
        message.setSubject(resolvedSubject);
        message.setText(resolvedBody);
        message.setFrom(resolveFrom(settings));
        if (settings.getSmtpUser() != null && !settings.getSmtpUser().isBlank()) {
            message.setReplyTo(settings.getSmtpUser());
        }

        try {
            sender.send(message);
        } catch (Exception ex) {
            String detail = ex.getMessage() != null ? ex.getMessage() : "Unknown SMTP error";
            throw new BadRequestException("SMTP test failed: " + detail);
        }
    }

    public NotificationSendResult sendLicensesDelivered(Sale sale, AccountsReceivable ar, java.util.List<License> licenses) {
        if (sale == null || sale.getClient() == null) {
            return new NotificationSendResult(false, "Sale or client is missing.");
        }
        IntegrationSettings settings = settingsService.getSettingsEntity();
        if (settings == null || !settings.isSmtpEnabled()) {
            return new NotificationSendResult(false, "SMTP is disabled.");
        }
        String to = sale.getClient().getEmail();
        if (to == null || to.isBlank()) {
            return new NotificationSendResult(false, "Client email is empty.");
        }
        if (licenses == null || licenses.isEmpty()) {
            return new NotificationSendResult(false, "No delivered licenses.");
        }

        JavaMailSenderImpl sender = buildSender(settings);
        if (sender == null) {
            return new NotificationSendResult(false, "SMTP settings are incomplete.");
        }

        Invoice invoice = invoiceRepository.findBySaleId(sale.getId()).orElse(null);

        Map<String, String> vars = new HashMap<>();
        vars.put("clientName", safeName(sale));
        vars.put("licenses", formatLicenseSummary(licenses));
        vars.put("licenseCardsHtml", formatLicenseCardsHtml(licenses));
        vars.put("invoiceNumber", invoice != null ? invoice.getInvoiceNumber() : "");
        vars.put("pdfUrl", "");
        putSaleIdentity(vars, sale);
        vars.put("saleDate", sale.getCreatedAt() != null ? sale.getCreatedAt().format(DATE_FORMATTER) : LocalDate.now().format(DATE_FORMATTER));
        vars.put("items", formatSaleItems(sale));
        vars.put("total", formatAmount(sale.getTotal()));
        vars.put("balance", formatAmount(ar != null ? ar.getBalance() : BigDecimal.ZERO));
        vars.put("dueDate", ar != null && ar.getDueDate() != null ? ar.getDueDate().toString() : "");
        vars.put("paymentStatus", ar != null && ar.getBalance() != null && ar.getBalance().signum() > 0 ? "Saldo pendiente" : "Pagado");
        vars.put("companyName", COMPANY_NAME);
        vars.put("supportEmail", SUPPORT_EMAIL);

        NotificationTemplateService.ResolvedTemplate template =
                templateService.resolveTemplates("LICENSES_DELIVERED", vars);

        String subject = template.emailSubject() != null ? template.emailSubject() : "Tus licencias digitales";
        String body = template.emailBody() != null
                ? template.emailBody()
                : defaultLicensesBody(vars, invoice != null ? invoice.getInvoiceNumber() : null);
        String htmlBody = template.emailBody() != null && looksLikeHtml(template.emailBody())
                ? template.emailBody()
                : defaultLicensesHtml(vars, invoice != null ? invoice.getInvoiceNumber() : null);

        try {
            sendEmail(sender, settings, to, subject, body, htmlBody);
            return new NotificationSendResult(true, "Email sent to " + to + ".");
        } catch (Exception ex) {
            log.warn("Failed to send licenses delivered email to {}", to, ex);
            return new NotificationSendResult(false, ex.getMessage() != null ? ex.getMessage() : "Unknown SMTP error.");
        }
    }

    public NotificationSendResult sendArReminder(com.dismal.distribuciones.modules.security.domain.User client,
                                                 BigDecimal balance,
                                                 long daysOverdue,
                                                 LocalDate dueDate) {
        if (client == null) {
            return new NotificationSendResult(false, "Client is missing.");
        }
        IntegrationSettings settings = settingsService.getSettingsEntity();
        if (settings == null || !settings.isSmtpEnabled()) {
            return new NotificationSendResult(false, "SMTP is disabled.");
        }
        if (client.getEmail() == null || client.getEmail().isBlank()) {
            return new NotificationSendResult(false, "Client email is empty.");
        }
        JavaMailSenderImpl sender = buildSender(settings);
        if (sender == null) {
            return new NotificationSendResult(false, "SMTP settings are incomplete.");
        }

        Map<String, String> vars = buildArVariables(client, balance, daysOverdue, dueDate);
        NotificationTemplateService.ResolvedTemplate template =
                templateService.resolveTemplates("AR_OVERDUE", vars);
        String subject = template.emailSubject() != null && !template.emailSubject().isBlank()
                ? template.emailSubject()
                : "Recordatorio de saldo pendiente";
        String body = template.emailBody() != null && !template.emailBody().isBlank()
                ? template.emailBody()
                : defaultArReminderBody(vars);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(client.getEmail());
        message.setSubject(subject);
        message.setText(body);
        message.setFrom(resolveFrom(settings));
        if (settings.getSmtpUser() != null && !settings.getSmtpUser().isBlank()) {
            message.setReplyTo(settings.getSmtpUser());
        }

        try {
            sender.send(message);
            return new NotificationSendResult(true, "Email sent to " + client.getEmail() + ".");
        } catch (Exception ex) {
            return new NotificationSendResult(false, ex.getMessage() != null ? ex.getMessage() : "Unknown SMTP error.");
        }
    }

    private JavaMailSenderImpl buildSender(IntegrationSettings settings) {
        if (settings.getSmtpHost() == null || settings.getSmtpHost().isBlank()) {
            return null;
        }
        if (settings.getSmtpPort() == null || settings.getSmtpPort() <= 0) {
            return null;
        }
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(settings.getSmtpHost());
        sender.setPort(settings.getSmtpPort());
        if (settings.getSmtpUser() != null && !settings.getSmtpUser().isBlank()) {
            sender.setUsername(settings.getSmtpUser());
        }
        if (settings.getSmtpPassword() != null && !settings.getSmtpPassword().isBlank()) {
            sender.setPassword(settings.getSmtpPassword());
        }
        Properties props = sender.getJavaMailProperties();
        boolean authEnabled = settings.getSmtpUser() != null && !settings.getSmtpUser().isBlank();
        props.put("mail.smtp.auth", Boolean.toString(authEnabled));
        boolean useSsl = settings.getSmtpPort() != null && settings.getSmtpPort() == 465;
        if (useSsl) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.starttls.enable", "false");
        } else {
            props.put("mail.smtp.starttls.enable", Boolean.toString(settings.isSmtpTls()));
        }
        props.put("mail.smtp.timeout", "15000");
        props.put("mail.smtp.connectiontimeout", "15000");
        return sender;
    }

    private String resolveFrom(IntegrationSettings settings) {
        String fromEmail = settings.getSmtpFromEmail();
        if (fromEmail == null || fromEmail.isBlank()) {
            return settings.getSmtpUser();
        }
        String fromName = settings.getSmtpFromName();
        if (fromName != null && !fromName.isBlank()) {
            return fromName + " <" + fromEmail + ">";
        }
        return fromEmail;
    }

    private String safeName(Sale sale) {
        String first = sale.getClient().getFirstname() != null ? sale.getClient().getFirstname() : "";
        String last = sale.getClient().getLastname() != null ? sale.getClient().getLastname() : "";
        String full = (first + " " + last).trim();
        return full.isBlank() ? sale.getClient().getEmail() : full;
    }

    private String resolveDefaultRecipient(IntegrationSettings settings) {
        if (settings.getSmtpFromEmail() != null && !settings.getSmtpFromEmail().isBlank()) {
            return settings.getSmtpFromEmail();
        }
        return settings.getSmtpUser();
    }

    private String formatAmount(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private void putSaleIdentity(Map<String, String> vars, Sale sale) {
        String saleUuid = sale.getId() != null ? sale.getId().toString() : "";
        String saleNumber = SaleNumberService.format(sale.getCountry(), sale.getSaleNumber());
        String publicReference = saleNumber != null ? saleNumber : saleUuid;
        vars.put("saleId", publicReference);
        vars.put("saleNumber", publicReference);
        vars.put("saleUuid", saleUuid);
    }

    private String safeName(com.dismal.distribuciones.modules.security.domain.User client) {
        String first = client.getFirstname() != null ? client.getFirstname() : "";
        String last = client.getLastname() != null ? client.getLastname() : "";
        String full = (first + " " + last).trim();
        return full.isBlank() ? client.getEmail() : full;
    }

    private String defaultBody(Map<String, String> vars, LocalDate dueDate) {
        StringBuilder builder = new StringBuilder();
        builder.append("Hola ").append(vars.getOrDefault("clientName", "cliente")).append(",\n\n");
        builder.append("Confirmamos tu compra en ").append(COMPANY_NAME).append(".\n\n");
        builder.append("Resumen de la venta\n");
        builder.append("Venta: ").append(vars.getOrDefault("saleId", "")).append("\n");
        builder.append("Fecha: ").append(vars.getOrDefault("saleDate", "")).append("\n");
        builder.append("Estado de pago: ").append(vars.getOrDefault("paymentStatus", "")).append("\n");
        builder.append("Total: $").append(vars.getOrDefault("total", "0.00")).append(" USD\n");
        String items = vars.getOrDefault("items", "");
        if (!items.isBlank()) {
            builder.append("\nProductos\n");
            builder.append(items).append("\n");
        }
        if (dueDate != null) {
            builder.append("\nSaldo pendiente: $").append(vars.getOrDefault("balance", "0.00")).append(" USD\n");
            builder.append("Fecha de vencimiento: ").append(dueDate).append("\n");
        }
        builder.append("\nTus licencias se enviaran en un correo separado cuando queden asignadas.\n");
        builder.append("Soporte: ").append(SUPPORT_EMAIL).append("\n\n");
        builder.append("Gracias por comprar en ").append(COMPANY_NAME).append(".");
        return builder.toString();
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
                    String subtotal = formatAmount(item.getSubtotal());
                    return "- " + name + " x" + quantity + " | Unitario: $" + unitPrice + " | Subtotal: $" + subtotal;
                })
                .collect(Collectors.joining("\n"));
    }

    private String formatLicenseSummary(List<License> licenses) {
        if (licenses == null || licenses.isEmpty()) {
            return "";
        }
        Map<LicenseDeliveryLine, Integer> grouped = groupDeliveredLicenses(licenses);
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
                .collect(Collectors.joining("\n\n"));
    }

    private String formatLicenseCardsHtml(List<License> licenses) {
        Map<LicenseDeliveryLine, Integer> grouped = groupDeliveredLicenses(licenses);
        return grouped.entrySet().stream()
                .map(entry -> {
                    LicenseDeliveryLine line = entry.getKey();
                    String activations = entry.getValue() > 1
                            ? entry.getValue() + " dispositivos / activaciones"
                            : "1 dispositivo / activacion";
                    return """
                            <tr>
                              <td style="padding:18px 0;border-bottom:1px solid #e5e7eb;">
                                <div style="font-size:16px;font-weight:700;color:#111827;">%s</div>
                                <div style="margin-top:6px;font-size:13px;line-height:1.5;color:#4b5563;">%s</div>
                                <div style="margin-top:12px;font-size:12px;font-weight:700;text-transform:uppercase;letter-spacing:.06em;color:#6b7280;">Serial</div>
                                <div style="margin-top:4px;padding:12px 14px;border-radius:10px;background:#f9fafb;border:1px solid #e5e7eb;font-family:Consolas,Menlo,monospace;font-size:15px;color:#111827;">%s</div>
                                <div style="margin-top:10px;font-size:13px;color:#374151;">Uso incluido: <strong>%s</strong></div>
                              </td>
                            </tr>
                            """.formatted(
                            escapeHtml(line.productName()),
                            escapeHtml(line.description().isBlank() ? "Producto digital adquirido en Dismal." : line.description()),
                            escapeHtml(line.serial()),
                            escapeHtml(activations)
                    );
                })
                .collect(Collectors.joining());
    }

    private Map<LicenseDeliveryLine, Integer> groupDeliveredLicenses(List<License> licenses) {
        Map<LicenseDeliveryLine, Integer> grouped = new LinkedHashMap<>();
        for (License license : licenses) {
            Software software = license.getSoftware();
            String name = software != null && software.getName() != null ? software.getName() : "Producto";
            String description = software != null && software.getDescription() != null ? software.getDescription().trim() : "";
            String key = license.getLicenseKey() != null ? license.getLicenseKey() : "";
            LicenseDeliveryLine line = new LicenseDeliveryLine(name, description, key);
            grouped.put(line, grouped.getOrDefault(line, 0) + 1);
        }
        return grouped;
    }

    private String defaultLicensesBody(Map<String, String> vars, String invoiceNumber) {
        StringBuilder builder = new StringBuilder();
        builder.append("Hola ").append(vars.getOrDefault("clientName", "cliente")).append(",\n\n");
        builder.append("Tus licencias digitales ya estan listas.\n\n");
        builder.append("Detalle de productos y seriales\n");
        builder.append(vars.getOrDefault("licenses", "")).append("\n\n");
        builder.append("Resumen\n");
        builder.append("Venta: ").append(vars.getOrDefault("saleId", "")).append("\n");
        builder.append("Total: $").append(vars.getOrDefault("total", "0.00")).append(" USD\n");
        if (invoiceNumber != null && !invoiceNumber.isBlank()) {
            builder.append("Factura: ").append(invoiceNumber).append("\n");
        }
        String balance = vars.getOrDefault("balance", "0.00");
        if (!"0.00".equals(balance)) {
            builder.append("Saldo pendiente: $").append(balance).append(" USD\n");
            String dueDate = vars.getOrDefault("dueDate", "");
            if (!dueDate.isBlank()) {
                builder.append("Vence: ").append(dueDate).append("\n");
            }
        }
        builder.append("\nRecomendacion: guarda este correo en un lugar seguro.\n");
        builder.append("Si necesitas ayuda con la activacion, responde este correo o escribe a ").append(SUPPORT_EMAIL).append(".\n\n");
        builder.append("Gracias por comprar en ").append(COMPANY_NAME).append(".");
        return builder.toString();
    }

    private String defaultLicensesHtml(Map<String, String> vars, String invoiceNumber) {
        String invoiceLine = invoiceNumber != null && !invoiceNumber.isBlank()
                ? "<div style=\"color:#4b5563;\">Factura: <strong>" + escapeHtml(invoiceNumber) + "</strong></div>"
                : "";
        String balance = vars.getOrDefault("balance", "0.00");
        String balanceLine = !"0.00".equals(balance)
                ? "<div style=\"color:#4b5563;\">Saldo pendiente: <strong>$" + escapeHtml(balance) + " USD</strong></div>"
                : "";
        return """
                <!doctype html>
                <html>
                <body style="margin:0;background:#f3f4f6;padding:28px 0;font-family:Arial,Helvetica,sans-serif;color:#111827;">
                  <div style="max-width:680px;margin:0 auto;background:#ffffff;border-radius:18px;overflow:hidden;border:1px solid #e5e7eb;">
                    <div style="padding:28px 32px;background:#111827;color:#ffffff;">
                      <div style="font-size:13px;letter-spacing:.08em;text-transform:uppercase;color:#d1d5db;">%s</div>
                      <h1 style="margin:10px 0 0;font-size:26px;line-height:1.2;">Tus licencias digitales estan listas</h1>
                    </div>
                    <div style="padding:30px 32px;">
                      <p style="margin:0 0 18px;font-size:16px;line-height:1.6;">Hola <strong>%s</strong>, confirmamos tu compra y te enviamos el detalle de los productos entregados.</p>
                      <div style="display:block;margin:0 0 22px;padding:16px;border-radius:14px;background:#f9fafb;border:1px solid #e5e7eb;">
                        <div style="color:#4b5563;">Venta: <strong>%s</strong></div>
                        <div style="color:#4b5563;">Total: <strong>$%s USD</strong></div>
                        <div style="color:#4b5563;">Estado de pago: <strong>%s</strong></div>
                        %s
                        %s
                      </div>
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="border-collapse:collapse;">
                        %s
                      </table>
                      <p style="margin:24px 0 0;font-size:14px;line-height:1.6;color:#4b5563;">Guarda este correo en un lugar seguro. Si necesitas ayuda con la activacion, responde este mensaje o escribe a <strong>%s</strong>.</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(COMPANY_NAME),
                escapeHtml(vars.getOrDefault("clientName", "cliente")),
                escapeHtml(vars.getOrDefault("saleId", "")),
                escapeHtml(vars.getOrDefault("total", "0.00")),
                escapeHtml(vars.getOrDefault("paymentStatus", "")),
                invoiceLine,
                balanceLine,
                vars.getOrDefault("licenseCardsHtml", ""),
                escapeHtml(SUPPORT_EMAIL)
        );
    }

    private Map<String, String> buildArVariables(com.dismal.distribuciones.modules.security.domain.User client,
                                                 BigDecimal balance,
                                                 long daysOverdue,
                                                 LocalDate dueDate) {
        Map<String, String> vars = new HashMap<>();
        vars.put("clientName", safeName(client));
        vars.put("balance", formatAmount(balance));
        vars.put("daysOverdue", String.valueOf(Math.max(daysOverdue, 0)));
        vars.put("dueDate", dueDate != null ? dueDate.toString() : "");
        vars.put("companyName", COMPANY_NAME);
        vars.put("supportEmail", SUPPORT_EMAIL);
        return vars;
    }

    private String defaultArReminderBody(Map<String, String> vars) {
        StringBuilder builder = new StringBuilder();
        builder.append("Hola ").append(vars.getOrDefault("clientName", "cliente")).append(",\n\n");
        builder.append("Te recordamos que tienes un saldo pendiente de $")
                .append(vars.getOrDefault("balance", "0.00"))
                .append(" USD en ").append(COMPANY_NAME).append(".\n");
        String dueDate = vars.getOrDefault("dueDate", "");
        if (!dueDate.isBlank()) {
            builder.append("Fecha de vencimiento: ").append(dueDate).append("\n");
        }
        String daysOverdue = vars.getOrDefault("daysOverdue", "0");
        if (!"0".equals(daysOverdue)) {
            builder.append("Dias vencidos: ").append(daysOverdue).append("\n");
        }
        builder.append("\nSi ya realizaste el pago, por favor ignora este mensaje o comparte el comprobante.\n");
        builder.append("Soporte: ").append(SUPPORT_EMAIL);
        return builder.toString();
    }

    private void sendEmail(JavaMailSenderImpl sender, IntegrationSettings settings, String to,
                           String subject, String plainBody, String htmlBody) throws Exception {
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setFrom(resolveFrom(settings));
        if (settings.getSmtpUser() != null && !settings.getSmtpUser().isBlank()) {
            helper.setReplyTo(settings.getSmtpUser());
        }
        if (htmlBody != null && !htmlBody.isBlank()) {
            helper.setText(plainBody != null ? plainBody : "", htmlBody);
        } else {
            helper.setText(plainBody != null ? plainBody : "", false);
        }
        sender.send(message);
    }

    private boolean looksLikeHtml(String value) {
        return value != null && value.contains("<") && value.contains(">");
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private record LicenseDeliveryLine(String productName, String description, String serial) {}
}
