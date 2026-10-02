package com.dismal.distribuciones.modules.sales.listener;

import com.dismal.distribuciones.modules.sales.event.OrderPaidEvent;
import com.dismal.distribuciones.modules.integrations.settings.service.IntegrationSettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class N8nWebhookListener {

    private final WebClient webClient;
    private final IntegrationSettingsService settingsService;

    public N8nWebhookListener(WebClient.Builder webClientBuilder, IntegrationSettingsService settingsService) {
        this.webClient = webClientBuilder.build();
        this.settingsService = settingsService;
    }

    // Using a private record for the DTO is a clean, modern approach.
    private record N8nPayload(String customerPhone, String softwareName, String licenseSerial) {}

    @Async // This ensures the webhook call doesn't block the main purchase thread.
    @EventListener(OrderPaidEvent.class)
    public void handleOrderPaidEvent(OrderPaidEvent event) {
        log.info("OrderPaidEvent received for customer: {}. Preparing to send webhook to n8n.", event.getCustomerEmail());

        String webhookUrl = settingsService.getSettingsEntity().getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("Skipping n8n webhook: webhook URL not configured.");
            return;
        }

        // Create the payload for the webhook.
        N8nPayload payload = new N8nPayload(
                event.getCustomerPhone(),
                event.getSoftwareName(),
                event.getLicenseSerial()
        );

        webClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(payload), N8nPayload.class)
                .retrieve()
                .toBodilessEntity() // We don't need the response body, just the status.
                .doOnSuccess(response -> log.info("Successfully sent webhook to n8n for order. Status code: {}", response.getStatusCode()))
                .doOnError(error -> log.error("Failed to send webhook to n8n. Error: {}", error.getMessage()))
                .subscribe(); // Subscribe is necessary to trigger the reactive call.
    }
}
