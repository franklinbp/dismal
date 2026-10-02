package com.dismal.distribuciones.modules.store.dto;

import java.util.List;

public record BulkLicenseRequest(
        List<CreateLicenseRequest> licenses
) {}
