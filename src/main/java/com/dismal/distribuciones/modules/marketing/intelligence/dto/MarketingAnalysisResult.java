package com.dismal.distribuciones.modules.marketing.intelligence.dto;

import com.dismal.distribuciones.modules.marketing.intelligence.domain.MarketingDiagnosis;
import com.dismal.distribuciones.modules.marketing.intelligence.domain.MarketingPriority;
import com.dismal.distribuciones.modules.marketing.intelligence.domain.MarketingState;

import java.util.List;
import java.util.UUID;

public record MarketingAnalysisResult(
    UUID productId,
    String productName,
    MarketingState state,
    MarketingDiagnosis diagnosis,
    MarketingPriority priority,
    List<String> suggestedActions,
    String explanation
) {}
