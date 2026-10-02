package com.dismal.distribuciones.modules.marketing.dto;

import com.dismal.distribuciones.modules.marketing.domain.CampaignChannel;
import com.dismal.distribuciones.modules.security.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for creating a new marketing campaign.
 * Includes validation constraints.
 */
public record CampaignCreateRequest(
        @NotBlank String title,
        @NotBlank String messageBody,
        String imageUrl,
        @NotNull Role targetRole,
        @NotNull CampaignChannel channel,
        UUID productId,
        LocalDateTime scheduledAt
) {
}
