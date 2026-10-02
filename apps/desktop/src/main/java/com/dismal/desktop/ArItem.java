package com.dismal.desktop;

public record ArItem(
        String id,
        String clientName,
        String clientEmail,
        String dueDate,
        double total,
        double paid,
        double balance,
        String status
) {}
