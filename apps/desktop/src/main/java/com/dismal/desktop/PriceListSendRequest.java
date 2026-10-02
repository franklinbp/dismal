package com.dismal.desktop;

public record PriceListSendRequest(
        String toEmail,
        String subject,
        String whatsappPhone,
        Boolean includePdf,
        Boolean includeXlsx,
        String countryCode,
        String customerType
) {}
