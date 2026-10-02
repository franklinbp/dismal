package com.dismal.desktop;

public record RecentSale(
        String id,
        String date,
        String clientName,
        String clientEmail,
        String saleType,
        String status,
        double total,
        double paid,
        double balance
) {}
