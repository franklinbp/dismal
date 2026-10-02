package com.dismal.distribuciones.modules.marketing.intelligence.controller;

import com.dismal.distribuciones.modules.marketing.intelligence.dto.MarketingAnalysisResult;
import com.dismal.distribuciones.modules.marketing.intelligence.dto.StrategyOverviewResponse;
import com.dismal.distribuciones.modules.marketing.intelligence.service.MarketingIntelligenceService;
import com.dismal.distribuciones.modules.marketing.intelligence.service.StrategyOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/marketing/intelligence")
@RequiredArgsConstructor
public class MarketingIntelligenceController {

    private final MarketingIntelligenceService intelligenceService;
    private final StrategyOverviewService strategyOverviewService;

    @GetMapping("/analysis")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<List<MarketingAnalysisResult>> getProductAnalysis() {
        return ResponseEntity.ok(intelligenceService.analyzeAllProducts());
    }

    @GetMapping("/strategy-overview")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<StrategyOverviewResponse> getStrategyOverview() {
        return ResponseEntity.ok(strategyOverviewService.getOverview());
    }

    @org.springframework.web.bind.annotation.PostMapping("/interactions")
    public ResponseEntity<Void> recordInteraction(
            @org.springframework.web.bind.annotation.RequestParam java.util.UUID softwareId,
            @org.springframework.web.bind.annotation.RequestParam String type
    ) {
        intelligenceService.recordInteraction(softwareId, type);
        return ResponseEntity.ok().build();
    }
}
