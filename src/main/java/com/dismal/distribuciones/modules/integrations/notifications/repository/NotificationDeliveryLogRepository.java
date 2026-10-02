package com.dismal.distribuciones.modules.integrations.notifications.repository;

import com.dismal.distribuciones.modules.integrations.notifications.domain.NotificationChannel;
import com.dismal.distribuciones.modules.integrations.notifications.domain.NotificationDeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationDeliveryLogRepository extends JpaRepository<NotificationDeliveryLog, UUID> {
    Optional<NotificationDeliveryLog> findTopByOutboxEventIdAndChannelOrderByCreatedAtDesc(
            UUID outboxEventId,
            NotificationChannel channel
    );

    List<NotificationDeliveryLog> findByOutboxEventIdOrderByCreatedAtDesc(UUID outboxEventId);

    void deleteByOutboxEventIdIn(List<UUID> outboxEventIds);
}
