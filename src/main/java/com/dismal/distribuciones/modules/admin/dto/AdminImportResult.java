package com.dismal.distribuciones.modules.admin.dto;

import java.util.List;

public record AdminImportResult(
        int imported,
        int skipped,
        List<String> errors
) {}
