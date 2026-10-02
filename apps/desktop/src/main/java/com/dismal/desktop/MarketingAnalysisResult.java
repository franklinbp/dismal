package com.dismal.desktop;

import java.util.List;

public record MarketingAnalysisResult(
    String productId,
    String productName,
    String state,
    String diagnosis,
    String priority,
    List<String> suggestedActions,
    String explanation
) {}
