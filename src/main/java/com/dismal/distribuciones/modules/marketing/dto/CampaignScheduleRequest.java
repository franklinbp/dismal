package com.dismal.distribuciones.modules.marketing.dto;

import java.time.LocalDateTime;

public record CampaignScheduleRequest(
        LocalDateTime scheduledAt
) {}
