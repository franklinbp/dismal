package com.dismal.distribuciones.modules.marketing.dto;

import com.dismal.distribuciones.modules.marketing.domain.CampaignChannel;
import com.dismal.distribuciones.modules.marketing.domain.CampaignStatus;
import com.dismal.distribuciones.modules.security.domain.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record CampaignResponse(
        UUID id,
        String title,
        String messageBody,
        String imageUrl,
        Role targetRole,
        CampaignChannel channel,
        UUID productId,
        String productName,
        LocalDateTime scheduledAt,
        CampaignStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
