package com.dismal.distribuciones.modules.reports.dto;

public record PriceListSendRequest(
        String toEmail,
        String subject,
        String clientName,
        String whatsappPhone,
        Boolean includePdf,
        Boolean includeXlsx,
        Boolean includeEcFinalPrice,
        Boolean includeEcDistributorPrice,
        Boolean includePeFinalPrice,
        Boolean includePeDistributorPrice,
        Boolean includeStock,
        Boolean sendWhatsapp,
        String countryCode,
        String customerType
) {}
