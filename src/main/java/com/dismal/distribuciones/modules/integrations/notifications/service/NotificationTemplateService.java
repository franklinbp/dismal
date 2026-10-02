package com.dismal.distribuciones.modules.integrations.notifications.service;

import com.dismal.distribuciones.modules.integrations.notifications.domain.NotificationChannel;
import com.dismal.distribuciones.modules.integrations.notifications.domain.NotificationTemplate;
import com.dismal.distribuciones.modules.integrations.notifications.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationTemplateService {

    private final NotificationTemplateRepository templateRepository;

    public ResolvedTemplate resolveTemplates(String eventType, Map<String, String> variables) {
        String emailSubject = null;
        String emailBody = null;
        String whatsappText = null;

        Optional<NotificationTemplate> emailTemplate =
                templateRepository.findByEventTypeAndChannelAndEnabledTrue(eventType, NotificationChannel.EMAIL);
        if (emailTemplate.isPresent()) {
            NotificationTemplate template = emailTemplate.get();
            emailSubject = replaceVars(template.getSubject(), variables);
            emailBody = replaceVars(template.getBody(), variables);
        }

        Optional<NotificationTemplate> whatsappTemplate =
                templateRepository.findByEventTypeAndChannelAndEnabledTrue(eventType, NotificationChannel.WHATSAPP);
        if (whatsappTemplate.isPresent()) {
            NotificationTemplate template = whatsappTemplate.get();
            whatsappText = replaceVars(template.getBody(), variables);
        }

        return new ResolvedTemplate(emailSubject, emailBody, whatsappText);
    }

    private String replaceVars(String template, Map<String, String> variables) {
        if (template == null) {
            return null;
        }
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace("{" + entry.getKey() + "}", value);
        }
        return result;
    }

    public record ResolvedTemplate(String emailSubject, String emailBody, String whatsappText) {}
}
