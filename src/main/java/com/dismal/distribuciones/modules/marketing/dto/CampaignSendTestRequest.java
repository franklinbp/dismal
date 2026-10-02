package com.dismal.distribuciones.modules.marketing.dto;

import com.dismal.distribuciones.modules.marketing.domain.CampaignChannel;

import java.util.UUID;

public record CampaignSendTestRequest(
        UUID clientId,
        CampaignChannel channel
) {}
