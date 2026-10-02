package com.dismal.distribuciones.modules.integrations.outbox.service;

import com.dismal.distribuciones.modules.integrations.notifications.domain.NotificationChannel;
import com.dismal.distribuciones.modules.integrations.notifications.domain.NotificationDeliveryLog;
import com.dismal.distribuciones.modules.integrations.notifications.domain.NotificationDeliveryStatus;
import com.dismal.distribuciones.modules.integrations.notifications.repository.NotificationDeliveryLogRepository;
import com.dismal.distribuciones.modules.integrations.outbox.domain.EventOutbox;
import com.dismal.distribuciones.modules.integrations.outbox.domain.EventOutboxStatus;
import com.dismal.distribuciones.modules.integrations.outbox.repository.EventOutboxRepository;
import com.dismal.distribuciones.modules.integrations.settings.domain.IntegrationSettings;
import com.dismal.distribuciones.modules.integrations.settings.service.IntegrationSettingsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class OutboxDispatcher {

    private static final long CLAIM_TTL_SECONDS = 60;

    private final EventOutboxRepository eventOutboxRepository;
    private final NotificationDeliveryLogRepository deliveryLogRepository;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final IntegrationSettingsService settingsService;
    private final TransactionTemplate transactionTemplate;
    private final AtomicLong lastDispatchAt = new AtomicLong(0);

    public OutboxDispatcher(EventOutboxRepository eventOutboxRepository,
                            NotificationDeliveryLogRepository deliveryLogRepository,
                            WebClient.Builder webClientBuilder,
                            ObjectMapper objectMapper,
                            IntegrationSettingsService settingsService,
                            PlatformTransactionManager transactionManager) {
        this.eventOutboxRepository = eventOutboxRepository;
        this.deliveryLogRepository = deliveryLogRepository;
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
        this.settingsService = settingsService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Scheduled(fixedDelayString = "${outbox.dispatch.rate-ms:5000}")
    public void dispatch() {
        IntegrationSettings settings = settingsService.getSettingsEntity();
        if (!settings.isDispatchEnabled()) {
            return;
        }

        long now = Instant.now().toEpochMilli();
        long last = lastDispatchAt.get();
        if (settings.getDispatchRateMs() > 0 && now - last < settings.getDispatchRateMs()) {
            return;
        }
        lastDispatchAt.set(now);

        List<EventOutbox> events = claimReadyEvents(settings);

        for (EventOutbox event : events) {
            if (event.getAttempts() >= settings.getDispatchMaxAttempts()) {
                continue;
            }
            sendEvent(event, settings);
        }
    }

    public boolean dispatchNow(UUID outboxId) {
        return eventOutboxRepository.findById(outboxId)
                .map(event -> {
                    sendEvent(event, settingsService.getSettingsEntity());
                    return true;
                })
                .orElse(false);
    }

    private void sendEvent(EventOutbox event, IntegrationSettings settings) {
        if (settings.getWebhookUrl() == null || settings.getWebhookUrl().isBlank()) {
            scheduleRetry(event.getId(), "Webhook URL not configured", null, null);
            return;
        }
        String payload = event.getPayloadJson();
        String idempotencyKey = resolveIdempotencyKey(event, payload);
        String signature = buildSignature(payload, settings.getSecret());

        try {
            DispatchResult result = webClient.post()
                    .uri(settings.getWebhookUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Event-Type", event.getEventType())
                    .header("X-Idempotency-Key", idempotencyKey)
                    .headers(headers -> {
                        if (signature != null) {
                            headers.set("X-Signature", signature);
                        }
                    })
                    .body(Mono.just(payload), String.class)
                    .exchangeToMono(response -> response.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(body -> new DispatchResult(response.statusCode(), body)))
                    .block();

            if (result != null && result.statusCode != null && result.statusCode.is2xxSuccessful()) {
                markSuccess(event.getId(), result);
            } else {
                Integer code = result != null && result.statusCode != null ? result.statusCode.value() : null;
                scheduleRetry(event.getId(), "Non-2xx status", code, result != null ? result.body : null);
            }
        } catch (Exception ex) {
            scheduleRetry(event.getId(), ex.getMessage(), null, null);
        }
    }

    private void scheduleRetry(UUID outboxId, String reason, Integer responseCode, String responseBody) {
        transactionTemplate.executeWithoutResult(status -> {
            EventOutbox event = eventOutboxRepository.findById(outboxId).orElse(null);
            if (event == null) {
                return;
            }
            int nextAttempt = event.getAttempts() + 1;
            event.setAttempts(nextAttempt);
            event.setStatus(EventOutboxStatus.FAILED);
            event.setNextAttemptAt(Instant.now().plusSeconds(backoffSeconds(nextAttempt)));
            event.setLastError(reason);
            event.setLastResponseCode(responseCode);
            event.setLastResponseBody(truncateBody(responseBody));
            eventOutboxRepository.save(event);
            createDeliveryLogs(event, NotificationDeliveryStatus.FAILED, reason);
            log.warn("Outbox dispatch failed outboxId={} eventType={} aggregateId={} attempts={} responseCode={} error={}",
                    event.getId(), event.getEventType(), event.getAggregateId(), nextAttempt, responseCode, reason);
        });
    }

    private void markSuccess(UUID outboxId, DispatchResult result) {
        transactionTemplate.executeWithoutResult(status -> {
            EventOutbox event = eventOutboxRepository.findById(outboxId).orElse(null);
            if (event == null) {
                return;
            }
            event.setStatus(EventOutboxStatus.SENT);
            event.setSentAt(Instant.now());
            event.setNextAttemptAt(null);
            event.setLastError(null);
            event.setLastResponseCode(result.statusCode.value());
            event.setLastResponseBody(truncateBody(result.body));
            eventOutboxRepository.save(event);
            createDeliveryLogs(event, NotificationDeliveryStatus.SENT, null);
            log.info("Outbox sent outboxId={} eventType={} aggregateId={} attempts={} responseCode={}",
                    event.getId(), event.getEventType(), event.getAggregateId(),
                    event.getAttempts(), result.statusCode.value());
        });
    }

    private long backoffSeconds(int attempt) {
        long[] schedule = new long[] {15, 60, 300, 1800, 7200, 21600};
        if (attempt <= schedule.length) {
            return schedule[attempt - 1];
        }
        long seconds = schedule[schedule.length - 1];
        long exponential = seconds * (long) Math.pow(2, attempt - schedule.length);
        return Math.min(exponential, 86400);
    }

    private List<EventOutbox> claimReadyEvents(IntegrationSettings settings) {
        List<EventOutbox> claimed = transactionTemplate.execute(status -> {
            Instant now = Instant.now();
            List<EventOutbox> events = eventOutboxRepository.findReadyEvents(
                    List.of(EventOutboxStatus.PENDING, EventOutboxStatus.FAILED),
                    now,
                    PageRequest.of(0, 20)
            );
            if (events.isEmpty()) {
                return events;
            }
            Instant nextAttemptAt = now.plusSeconds(resolveClaimTtlSeconds(settings));
            for (EventOutbox event : events) {
                event.setNextAttemptAt(nextAttemptAt);
            }
            eventOutboxRepository.saveAll(events);
            return events;
        });
        return claimed != null ? claimed : List.of();
    }

    private long resolveClaimTtlSeconds(IntegrationSettings settings) {
        long rateMs = settings.getDispatchRateMs();
        long derived = rateMs > 0 ? (rateMs / 1000L) * 3 : CLAIM_TTL_SECONDS;
        return Math.max(CLAIM_TTL_SECONDS, derived);
    }

    private String buildSignature(String payload, String secret) {
        if (secret == null || secret.isBlank()) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.error("Failed to sign payload: {}", e.getMessage());
            return null;
        }
    }

    private String resolveIdempotencyKey(EventOutbox event, String payload) {
        try {
            Map<String, Object> parsed = objectMapper.readValue(payload, Map.class);
            Object meta = parsed.get("meta");
            if (meta instanceof Map<?, ?> metaMap) {
                Object key = metaMap.get("idempotencyKey");
                if (key != null && !key.toString().isBlank()) {
                    return key.toString();
                }
            }
        } catch (Exception ignored) {
        }
        return event.getEventType() + "-" + event.getAggregateId();
    }

    private String truncateBody(String body) {
        if (body == null) {
            return null;
        }
        int max = 2048;
        if (body.length() <= max) {
            return body;
        }
        return body.substring(0, max);
    }

    private void createDeliveryLogs(EventOutbox event, NotificationDeliveryStatus status, String error) {
        List<NotificationChannel> channels = resolveChannels(event.getPayloadJson());
        if (channels.isEmpty()) {
            return;
        }
        for (NotificationChannel channel : channels) {
            if (status == NotificationDeliveryStatus.FAILED
                    && deliveryLogRepository.findTopByOutboxEventIdAndChannelOrderByCreatedAtDesc(event.getId(), channel)
                    .map(existing -> existing.getStatus() == NotificationDeliveryStatus.SENT
                            || existing.getStatus() == NotificationDeliveryStatus.DELIVERED)
                    .orElse(false)) {
                continue;
            }
            NotificationDeliveryLog logEntry = NotificationDeliveryLog.builder()
                    .outboxEventId(event.getId())
                    .channel(channel)
                    .status(status)
                    .error(error)
                    .build();
            deliveryLogRepository.save(logEntry);
        }
    }

    private List<NotificationChannel> resolveChannels(String payload) {
        try {
            Map<String, Object> parsed = objectMapper.readValue(payload, Map.class);
            Object resolved = parsed.get("templateResolved");
            if (!(resolved instanceof Map<?, ?> resolvedMap)) {
                return List.of();
            }
            boolean hasEmail = hasText(resolvedMap.get("emailSubject")) || hasText(resolvedMap.get("emailBody"));
            boolean hasWhatsapp = hasText(resolvedMap.get("whatsappText"));
            if (hasEmail && hasWhatsapp) {
                return List.of(NotificationChannel.EMAIL, NotificationChannel.WHATSAPP);
            }
            if (hasEmail) {
                return List.of(NotificationChannel.EMAIL);
            }
            if (hasWhatsapp) {
                return List.of(NotificationChannel.WHATSAPP);
            }
            return List.of();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private boolean hasText(Object value) {
        return value != null && !value.toString().isBlank();
    }

    private record DispatchResult(HttpStatusCode statusCode, String body) {}
}
