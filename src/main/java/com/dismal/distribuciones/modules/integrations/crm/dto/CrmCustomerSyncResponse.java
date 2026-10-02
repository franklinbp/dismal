package com.dismal.distribuciones.modules.integrations.crm.dto;

import java.util.List;

public record CrmCustomerSyncResponse(
        boolean enabled,
        int processed,
        int created,
        int updated,
        int skipped,
        int disabled,
        int failed,
        List<String> errors
) {
}
