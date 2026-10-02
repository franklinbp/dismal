package com.dismal.distribuciones.modules.integrations.outbox.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "event_outbox")
public class EventOutbox {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private UUID aggregateId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventOutboxStatus status;

    @Column(nullable = false)
    private Integer attempts;

    private Instant nextAttemptAt;

    private Instant sentAt;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    private Integer lastResponseCode;

    @Column(columnDefinition = "TEXT")
    private String lastResponseBody;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
