package com.dismal.distribuciones.modules.reports.service;

import com.dismal.distribuciones.exception.BadRequestException;
import com.dismal.distribuciones.modules.integrations.settings.domain.IntegrationSettings;
import com.dismal.distribuciones.modules.integrations.settings.service.IntegrationSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceListEmailService {

    private final IntegrationSettingsService settingsService;

    public void sendPriceList(
            String to,
            String subject,
            String htmlBody,
            byte[] pdf,
            byte[] xlsx
    ) {
        IntegrationSettings settings = settingsService.getSettingsEntity();
        if (settings == null || !settings.isSmtpEnabled()) {
            throw new BadRequestException("SMTP is disabled.");
        }
        JavaMailSenderImpl sender = buildSender(settings);
        if (sender == null) {
            throw new BadRequestException("SMTP settings are incomplete.");
        }
        String resolvedTo = normalizeEmail(to);
        if (resolvedTo.isBlank()) {
            throw new BadRequestException("Recipient email is required.");
        }
        String resolvedSubject = subject != null && !subject.isBlank()
                ? subject.trim()
                : "Lista de precios - Dismal";

        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(resolvedTo);
            helper.setSubject(resolvedSubject);
            helper.setText(htmlBody, true);
            helper.setFrom(resolveFrom(settings));
            if (settings.getSmtpUser() != null && !settings.getSmtpUser().isBlank()) {
                helper.setReplyTo(settings.getSmtpUser());
            }
            if (pdf != null && pdf.length > 0) {
                helper.addAttachment("lista-precios.pdf", new org.springframework.core.io.ByteArrayResource(pdf));
            }
            if (xlsx != null && xlsx.length > 0) {
                helper.addAttachment("lista-precios.xlsx", new org.springframework.core.io.ByteArrayResource(xlsx));
            }
            sender.send(message);
        } catch (Exception ex) {
            log.warn("Failed to send price list email", ex);
            throw new BadRequestException("No se pudo enviar el email a " + resolvedTo + ": " + resolveEmailError(ex));
        }
    }

    private String normalizeEmail(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\u200B", "")
                .replace("\uFEFF", "")
                .trim();
    }

    private String resolveEmailError(Exception ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof jakarta.mail.SendFailedException sendFailedException
                    && sendFailedException.getInvalidAddresses() != null
                    && sendFailedException.getInvalidAddresses().length > 0) {
                return "direccion rechazada por el servidor SMTP: "
                        + sendFailedException.getInvalidAddresses()[0].toString();
            }
            String message = current.getMessage();
            if (message != null && !message.isBlank()) {
                return message;
            }
            current = current.getCause();
        }
        return "error SMTP desconocido.";
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
}
