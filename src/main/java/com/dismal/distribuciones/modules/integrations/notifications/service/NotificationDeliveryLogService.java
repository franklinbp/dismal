package com.dismal.distribuciones.modules.integrations.notifications.service;

import com.dismal.distribuciones.modules.integrations.notifications.domain.NotificationChannel;
import com.dismal.distribuciones.modules.integrations.notifications.domain.NotificationDeliveryLog;
import com.dismal.distribuciones.modules.integrations.notifications.domain.NotificationDeliveryStatus;
import com.dismal.distribuciones.modules.integrations.notifications.repository.NotificationDeliveryLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationDeliveryLogService {

    private final NotificationDeliveryLogRepository deliveryLogRepository;

    @Transactional
    public void markSent(UUID outboxEventId, NotificationChannel channel, String providerMessageId) {
        save(outboxEventId, channel, NotificationDeliveryStatus.SENT, providerMessageId, null);
    }

    @Transactional
    public void markFailed(UUID outboxEventId, NotificationChannel channel, String error) {
        save(outboxEventId, channel, NotificationDeliveryStatus.FAILED, null, error);
    }

    @Transactional(readOnly = true)
    public boolean hasSuccessfulDelivery(UUID outboxEventId, NotificationChannel channel) {
        if (outboxEventId == null || channel == null) {
            return false;
        }
        return deliveryLogRepository.findTopByOutboxEventIdAndChannelOrderByCreatedAtDesc(outboxEventId, channel)
                .map(log -> log.getStatus() == NotificationDeliveryStatus.SENT
                        || log.getStatus() == NotificationDeliveryStatus.DELIVERED)
                .orElse(false);
    }

    private void save(UUID outboxEventId, NotificationChannel channel, NotificationDeliveryStatus status,
                      String providerMessageId, String error) {
        if (outboxEventId == null || channel == null) {
            return;
        }
        NotificationDeliveryLog logEntry = NotificationDeliveryLog.builder()
                .outboxEventId(outboxEventId)
                .channel(channel)
                .status(status)
                .providerMessageId(providerMessageId)
                .error(status == NotificationDeliveryStatus.FAILED ? truncate(error) : null)
                .build();
        deliveryLogRepository.save(logEntry);
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        int max = 1000;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
