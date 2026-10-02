package com.dismal.desktop;

public record StrategyAction(
        String id,
        String productId,
        String productName,
        String targetId,
        String source,
        String priority,
        String status,
        String recommendedChannel,
        String title,
        String description,
        String assignedTo,
        String assignedToName,
        String dueDate,
        String completedAt,
        String resultNotes,
        String createdAt,
        String updatedAt
) {}
