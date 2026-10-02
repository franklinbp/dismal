package com.dismal.desktop;

import java.time.LocalDateTime;

public record PriceListSendResponse(
        LocalDateTime sentAt,
        String whatsappText,
        String whatsappUrl
) {}
