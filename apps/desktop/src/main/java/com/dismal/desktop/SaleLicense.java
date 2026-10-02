package com.dismal.desktop;

import java.time.LocalDateTime;

public record SaleLicense(
        String id,
        String licenseId,
        String licenseKey,
        String softwareId,
        String softwareName,
        LocalDateTime assignedAt
) {}
