package com.dismal.desktop;

import java.util.List;

public record AdminImportResult(
        int imported,
        int skipped,
        List<String> errors
) {}
