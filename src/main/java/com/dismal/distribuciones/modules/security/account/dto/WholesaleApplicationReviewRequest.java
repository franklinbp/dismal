package com.dismal.distribuciones.modules.security.account.dto;

import com.dismal.distribuciones.modules.security.account.domain.WholesaleApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WholesaleApplicationReviewRequest(
        @NotNull WholesaleApplicationStatus status,
        @Size(max = 1000) String reviewNotes
) {}
