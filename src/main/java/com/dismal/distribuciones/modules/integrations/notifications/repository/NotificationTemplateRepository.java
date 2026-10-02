package com.dismal.distribuciones.modules.integrations.notifications.repository;

import com.dismal.distribuciones.modules.integrations.notifications.domain.NotificationChannel;
import com.dismal.distribuciones.modules.integrations.notifications.domain.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {
    Optional<NotificationTemplate> findByEventTypeAndChannelAndEnabledTrue(String eventType, NotificationChannel channel);
}
