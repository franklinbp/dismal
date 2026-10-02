package com.dismal.distribuciones.modules.store.dto;

import com.dismal.distribuciones.modules.store.domain.LicenseStatus;

public record LicenseUpdateRequest(
        LicenseStatus status,
        Boolean available
) {}
