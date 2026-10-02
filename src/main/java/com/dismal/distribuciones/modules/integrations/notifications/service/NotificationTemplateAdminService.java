package com.dismal.distribuciones.modules.integrations.notifications.service;

import com.dismal.distribuciones.exception.BadRequestException;
import com.dismal.distribuciones.exception.ResourceNotFoundException;
import com.dismal.distribuciones.modules.integrations.notifications.domain.NotificationTemplate;
import com.dismal.distribuciones.modules.integrations.notifications.domain.NotificationDeliveryLog;
import com.dismal.distribuciones.modules.integrations.notifications.domain.NotificationDeliveryStatus;
import com.dismal.distribuciones.modules.integrations.notifications.dto.NotificationDeliveryCallbackRequest;
import com.dismal.distribuciones.modules.integrations.notifications.dto.NotificationDeliveryLogResponse;
import com.dismal.distribuciones.modules.integrations.notifications.dto.NotificationTemplateRequest;
import com.dismal.distribuciones.modules.integrations.notifications.dto.NotificationTemplateResponse;
import com.dismal.distribuciones.modules.integrations.notifications.repository.NotificationDeliveryLogRepository;
import com.dismal.distribuciones.modules.integrations.notifications.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationTemplateAdminService {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationDeliveryLogRepository deliveryLogRepository;

    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> list(String eventType, String channel, Boolean enabled) {
        return templateRepository.findAll().stream()
                .filter(template -> eventType == null || template.getEventType().equalsIgnoreCase(eventType))
                .filter(template -> channel == null || template.getChannel().name().equalsIgnoreCase(channel))
                .filter(template -> enabled == null || template.isEnabled() == enabled)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationTemplateResponse get(UUID id) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found."));
        return toResponse(template);
    }

    @Transactional
    public NotificationTemplateResponse create(NotificationTemplateRequest request) {
        validate(request);
        NotificationTemplate template = NotificationTemplate.builder()
                .eventType(request.eventType())
                .channel(request.channel())
                .subject(request.subject())
                .body(request.body())
                .enabled(request.enabled() != null ? request.enabled() : true)
                .build();
        return toResponse(templateRepository.save(template));
    }

    @Transactional
    public NotificationTemplateResponse update(UUID id, NotificationTemplateRequest request) {
        validate(request);
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found."));
        template.setEventType(request.eventType());
        template.setChannel(request.channel());
        template.setSubject(request.subject());
        template.setBody(request.body());
        template.setEnabled(request.enabled() != null ? request.enabled() : template.isEnabled());
        return toResponse(templateRepository.save(template));
    }

    @Transactional
    public void delete(UUID id) {
        if (!templateRepository.existsById(id)) {
            throw new ResourceNotFoundException("Template not found.");
        }
        templateRepository.deleteById(id);
    }

    @Transactional
    public NotificationDeliveryLogResponse upsertDeliveryLog(NotificationDeliveryCallbackRequest request) {
        if (request == null || request.outboxEventId() == null || request.channel() == null || request.status() == null) {
            throw new BadRequestException("outboxEventId, channel, status required.");
        }
        NotificationDeliveryLog logEntry = deliveryLogRepository
                .findTopByOutboxEventIdAndChannelOrderByCreatedAtDesc(request.outboxEventId(), request.channel())
                .orElse(NotificationDeliveryLog.builder()
                        .outboxEventId(request.outboxEventId())
                        .channel(request.channel())
                        .build());
        logEntry.setStatus(request.status());
        logEntry.setProviderMessageId(request.providerMessageId());
        logEntry.setError(request.status() == NotificationDeliveryStatus.FAILED ? request.error() : null);
        NotificationDeliveryLog saved = deliveryLogRepository.save(logEntry);
        return toDeliveryResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<NotificationDeliveryLogResponse> listDeliveryLogs(UUID outboxEventId) {
        if (outboxEventId == null) {
            return List.of();
        }
        return deliveryLogRepository.findByOutboxEventIdOrderByCreatedAtDesc(outboxEventId).stream()
                .map(this::toDeliveryResponse)
                .toList();
    }

    private void validate(NotificationTemplateRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body required.");
        }
        if (request.eventType() == null || request.eventType().isBlank()) {
            throw new BadRequestException("eventType required.");
        }
        if (request.channel() == null) {
            throw new BadRequestException("channel required.");
        }
        if (request.body() == null || request.body().isBlank()) {
            throw new BadRequestException("body required.");
        }
    }

    private NotificationTemplateResponse toResponse(NotificationTemplate template) {
        return new NotificationTemplateResponse(
                template.getId(),
                template.getEventType(),
                template.getChannel(),
                template.getSubject(),
                template.getBody(),
                template.isEnabled(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }

    private NotificationDeliveryLogResponse toDeliveryResponse(NotificationDeliveryLog log) {
        return new NotificationDeliveryLogResponse(
                log.getId(),
                log.getOutboxEventId(),
                log.getChannel(),
                log.getStatus(),
                log.getProviderMessageId(),
                log.getError(),
                log.getCreatedAt(),
                log.getUpdatedAt()
        );
    }
}
