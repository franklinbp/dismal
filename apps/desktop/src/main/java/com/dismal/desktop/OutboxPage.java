package com.dismal.desktop;

import java.util.List;

public record OutboxPage(
        List<OutboxItem> content,
        long totalElements,
        int totalPages,
        int size,
        int number
) {}
