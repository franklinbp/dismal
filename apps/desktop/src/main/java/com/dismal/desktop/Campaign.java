package com.dismal.desktop;

public record Campaign(
        String id,
        String title,
        String messageBody,
        String imageUrl,
        String targetRole,
        String channel,
        String productId,
        String productName,
        String scheduledAt,
        String status,
        String createdAt,
        String updatedAt
) {}
