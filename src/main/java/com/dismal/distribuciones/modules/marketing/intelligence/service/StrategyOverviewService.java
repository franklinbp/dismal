package com.dismal.distribuciones.modules.marketing.intelligence.service;

import com.dismal.distribuciones.modules.marketing.domain.CampaignStatus;
import com.dismal.distribuciones.modules.marketing.domain.StrategyActionStatus;
import com.dismal.distribuciones.modules.marketing.intelligence.domain.MarketingPriority;
import com.dismal.distribuciones.modules.marketing.intelligence.domain.MarketingState;
import com.dismal.distribuciones.modules.marketing.intelligence.dto.MarketingAnalysisResult;
import com.dismal.distribuciones.modules.marketing.intelligence.dto.StrategyItemResponse;
import com.dismal.distribuciones.modules.marketing.intelligence.dto.StrategyOverviewResponse;
import com.dismal.distribuciones.modules.marketing.repository.CampaignRepository;
import com.dismal.distribuciones.modules.marketing.repository.StrategyActionRepository;
import com.dismal.distribuciones.modules.planning.domain.SalesTarget;
import com.dismal.distribuciones.modules.planning.domain.SalesTargetStatus;
import com.dismal.distribuciones.modules.planning.repository.SalesTargetRepository;
import com.dismal.distribuciones.modules.planning.service.SalesTargetCalculatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StrategyOverviewService {

    private final SalesTargetRepository salesTargetRepository;
    private final SalesTargetCalculatorService calculatorService;
    private final MarketingIntelligenceService marketingIntelligenceService;
    private final CampaignRepository campaignRepository;
    private final StrategyActionRepository strategyActionRepository;

    @Transactional(readOnly = true)
    public StrategyOverviewResponse getOverview() {
        LocalDate today = LocalDate.now();
        Map<UUID, MarketingAnalysisResult> analysisByProduct = marketingIntelligenceService.analyzeAllProducts().stream()
                .collect(Collectors.toMap(MarketingAnalysisResult::productId, Function.identity(), (left, right) -> left));

        List<StrategyItemResponse> items = salesTargetRepository.findAll().stream()
                .filter(target -> isActiveTarget(target, today))
                .map(target -> toItem(target, analysisByProduct.get(target.getSoftware().getId()), today))
                .toList();

        int achievedTargets = (int) items.stream().filter(item -> item.progressPercent() >= 100.0).count();
        int targetsBehind = (int) items.stream().filter(StrategyItemResponse::behindSchedule).count();
        int highPriority = (int) items.stream().filter(item -> item.priority() == MarketingPriority.ALTA).count();
        int commercialAlerts = (int) items.stream().filter(item -> item.state() == MarketingState.ALERTA_COMERCIAL).count();
        int pausedProducts = (int) items.stream().filter(item -> item.state() == MarketingState.PAUSADO).count();
        BigDecimal activeRevenue = items.stream()
                .map(StrategyItemResponse::targetRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal activeExpectedProfit = items.stream()
                .map(StrategyItemResponse::expectedProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<StrategyItemResponse> priorityItems = items.stream()
                .sorted(Comparator
                        .comparingInt((StrategyItemResponse item) -> item.behindSchedule() ? 1 : 0).reversed()
                        .thenComparing(Comparator.comparingInt((StrategyItemResponse item) -> priorityWeight(item.priority())).reversed())
                        .thenComparingLong(StrategyItemResponse::daysRemaining))
                .limit(12)
                .toList();

        return new StrategyOverviewResponse(
                items.size(),
                achievedTargets,
                targetsBehind,
                highPriority,
                commercialAlerts,
                pausedProducts,
                campaignRepository.countByStatus(CampaignStatus.SCHEDULED),
                campaignRepository.countByStatus(CampaignStatus.FAILED),
                strategyActionRepository.countByStatusIn(List.of(
                        StrategyActionStatus.PENDIENTE,
                        StrategyActionStatus.EN_PROGRESO
                )),
                activeRevenue,
                activeExpectedProfit,
                priorityItems
        );
    }

    private StrategyItemResponse toItem(SalesTarget target, MarketingAnalysisResult analysis, LocalDate today) {
        var metrics = calculatorService.calculate(target);
        int metaUnits = safeInt(target.getMetaUnits());
        int soldUnits = safeInt(target.getUnitsSoldCurrent());
        double progressPercent = percent(soldUnits, metaUnits);
        double expectedPacePercent = expectedPace(target, today);
        boolean behindSchedule = progressPercent + 5.0 < expectedPacePercent;
        LocalDate deadline = target.getDeadline();
        long daysRemaining = deadline == null ? 0 : ChronoUnit.DAYS.between(today, deadline);
        MarketingState state = analysis != null ? analysis.state() : MarketingState.DESCONOCIDO;
        MarketingPriority priority = resolvePriority(analysis, behindSchedule, daysRemaining);
        List<String> suggestedActions = analysis != null ? analysis.suggestedActions() : List.of();

        return new StrategyItemResponse(
                target.getSoftware().getId(),
                target.getSoftware().getName(),
                state,
                priority,
                metaUnits,
                soldUnits,
                metrics.getTargetRevenue(),
                metrics.getExpectedProfit(),
                progressPercent,
                expectedPacePercent,
                behindSchedule,
                deadline,
                daysRemaining,
                recommendedAction(behindSchedule, state, suggestedActions),
                reason(target, analysis, progressPercent, expectedPacePercent),
                suggestedActions
        );
    }

    private boolean isActiveTarget(SalesTarget target, LocalDate today) {
        return safeInt(target.getMetaUnits()) > 0
                && target.getStatus() == SalesTargetStatus.ACTIVE
                && (target.getDeadline() == null || !target.getDeadline().isBefore(today));
    }

    private double expectedPace(SalesTarget target, LocalDate today) {
        if (target.getDeadline() == null || target.getCreatedAt() == null) {
            return 0.0;
        }
        LocalDate start = target.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
        if (!target.getDeadline().isAfter(start)) {
            return 100.0;
        }
        long totalDays = Math.max(1, ChronoUnit.DAYS.between(start, target.getDeadline()) + 1);
        long elapsedDays = Math.max(0, ChronoUnit.DAYS.between(start, today) + 1);
        return Math.min(100.0, (elapsedDays * 100.0) / totalDays);
    }

    private double percent(int value, int total) {
        if (total <= 0) {
            return 0.0;
        }
        BigDecimal result = BigDecimal.valueOf(value)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        return result.doubleValue();
    }

    private MarketingPriority resolvePriority(MarketingAnalysisResult analysis, boolean behindSchedule, long daysRemaining) {
        if (behindSchedule && daysRemaining <= 7) {
            return MarketingPriority.ALTA;
        }
        if (analysis != null && analysis.priority() != null) {
            return analysis.priority();
        }
        return behindSchedule ? MarketingPriority.MEDIA : MarketingPriority.NINGUNA;
    }

    private String recommendedAction(boolean behindSchedule, MarketingState state, List<String> suggestedActions) {
        if (behindSchedule) {
            return "Crear accion comercial hoy: campana, seguimiento o promocion dirigida.";
        }
        if (state == MarketingState.PAUSADO) {
            return "Revisar si conviene relanzar, ajustar precio o pausar la meta.";
        }
        if (suggestedActions != null && !suggestedActions.isEmpty()) {
            return suggestedActions.get(0);
        }
        return "Mantener seguimiento y revisar avance en el siguiente corte.";
    }

    private String reason(SalesTarget target, MarketingAnalysisResult analysis, double progressPercent, double expectedPacePercent) {
        String base = "Avance " + format(progressPercent) + "% vs ritmo esperado " + format(expectedPacePercent) + "%.";
        if (analysis == null || analysis.explanation() == null || analysis.explanation().isBlank()) {
            return base;
        }
        return base + " " + analysis.explanation();
    }

    private String format(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).toPlainString();
    }

    private int priorityWeight(MarketingPriority priority) {
        if (priority == MarketingPriority.ALTA) {
            return 3;
        }
        if (priority == MarketingPriority.MEDIA) {
            return 2;
        }
        if (priority == MarketingPriority.BAJA) {
            return 1;
        }
        return 0;
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }
}
