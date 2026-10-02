package com.dismal.distribuciones.modules.integrations.crm.service;

import com.dismal.distribuciones.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DismalCrmGateway {

    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(8);

    private final WebClient.Builder webClientBuilder;

    @Value("${integrations.dismal-crm.base-url:}")
    private String baseUrl;

    @Value("${integrations.dismal-crm.api-token:}")
    private String apiToken;

    public RemoteCampaignClient createCampaignClient(UpsertCampaignClientRequest request) {
        return buildClient()
                .post()
                .uri("/gateway/campaign-clients")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RemoteCampaignClient.class)
                .block();
    }

    public RemoteCampaignClient updateCampaignClient(Integer clientId, UpsertCampaignClientRequest request) {
        return buildClient()
                .put()
                .uri("/gateway/campaign-clients/{id}", clientId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RemoteCampaignClient.class)
                .block();
    }

    public void deleteCampaignClient(Integer clientId) {
        buildClient()
                .delete()
                .uri("/gateway/campaign-clients/{id}", clientId)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    public void sendWhatsAppMessage(SendWhatsAppMessageRequest request) {
        buildClient()
                .post()
                .uri("/gateway/messages/send")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> new BadRequestException(
                                        "DismalCRM WhatsApp API returned "
                                                + response.statusCode().value()
                                                + (body.isBlank() ? "" : ": " + body)
                                ))
                )
                .toBodilessEntity()
                .timeout(CONNECTION_TIMEOUT)
                .block();
    }

    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isBlank()
                && apiToken != null && !apiToken.isBlank();
    }

    public String getPublicEndpoint() {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public void verifyConnection() {
        buildClient()
                .get()
                .uri("/gateway/health")
                .retrieve()
                .toBodilessEntity()
                .timeout(CONNECTION_TIMEOUT)
                .block();
    }

    public RemoteCampaignClient findByPhone(String phoneE164) {
        return findFirstMatch(phoneE164, phoneE164, null);
    }

    public RemoteCampaignClient findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return findFirstMatch(email, null, email);
    }

    private RemoteCampaignClient findFirstMatch(String searchParam, String phoneE164, String email) {
        CampaignClientListResponse response = buildClient()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/gateway/campaign-clients")
                        .queryParam("searchParam", searchParam)
                        .queryParam("pageNumber", 1)
                        .build())
                .retrieve()
                .bodyToMono(CampaignClientListResponse.class)
                .block();

        if (response == null || response.clients() == null) {
            return null;
        }

        return response.clients().stream()
                .filter(client -> phoneE164 != null
                        ? Objects.equals(phoneE164, client.phoneE164())
                        : Objects.equals(normalize(email), normalize(client.email())))
                .findFirst()
                .orElse(null);
    }

    private WebClient buildClient() {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new BadRequestException("DismalCRM base URL is not configured.");
        }
        if (apiToken == null || apiToken.isBlank()) {
            throw new BadRequestException("DismalCRM API token is not configured.");
        }
        return webClientBuilder
                .baseUrl(baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .build();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    public record UpsertCampaignClientRequest(
            String name,
            String tradeName,
            String phone,
            String email,
            String category
    ) {
    }

    public record RemoteCampaignClient(
            Integer id,
            String name,
            String tradeName,
            String phoneE164,
            String email,
            String category
    ) {
    }

    public record SendWhatsAppMessageRequest(
            String number,
            String body,
            Integer whatsappId
    ) {
    }

    private record CampaignClientListResponse(
            List<RemoteCampaignClient> clients,
            long count,
            boolean hasMore
    ) {
    }
}
