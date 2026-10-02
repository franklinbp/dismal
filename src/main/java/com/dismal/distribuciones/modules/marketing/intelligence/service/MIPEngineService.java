package com.dismal.distribuciones.modules.marketing.intelligence.service;

import com.dismal.distribuciones.modules.marketing.intelligence.domain.MarketingDiagnosis;
import com.dismal.distribuciones.modules.marketing.intelligence.domain.MarketingPriority;
import com.dismal.distribuciones.modules.marketing.intelligence.domain.MarketingState;
import com.dismal.distribuciones.modules.marketing.intelligence.dto.MarketingAnalysisResult;
import com.dismal.distribuciones.modules.marketing.intelligence.dto.ProductMetrics;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MIPEngineService {

    public MarketingAnalysisResult analyzeProduct(UUID productId, String productName, ProductMetrics metrics) {
        MarketingState state = determineState(metrics);
        MarketingDiagnosis diagnosis = MarketingDiagnosis.OPTIMO;
        MarketingPriority priority = MarketingPriority.NINGUNA;
        List<String> actions = new ArrayList<>();
        String explanation = "";

        if (state == MarketingState.RENTABLE) {
            explanation = "El producto tiene un desempeno excelente. Mantener estrategia actual.";
        } else if (state == MarketingState.BAJA_TRACCION) {
            diagnosis = MarketingDiagnosis.PROBLEMA_VISIBILIDAD;
            priority = MarketingPriority.BAJA;
            explanation = "Ventas ocasionales detectadas. Se recomienda aumentar la visibilidad.";
            actions.addAll(getActionsForDiagnosis(diagnosis));
        } else if (state == MarketingState.ALERTA_COMERCIAL) {
            diagnosis = determineDiagnosis(metrics);
            priority = determinePriority(metrics);
            explanation = "ALERTA: Hay interes pero no se concretan ventas en 14 dias.";
            actions.addAll(getActionsForDiagnosis(diagnosis));
        } else if (state == MarketingState.PAUSADO) {
            diagnosis = MarketingDiagnosis.PROBLEMA_VISIBILIDAD;
            priority = MarketingPriority.BAJA;
            explanation = "Producto sin actividad comercial en 30 dias.";
            actions.add("Evaluar descontinuacion o relanzamiento total.");
        }

        return new MarketingAnalysisResult(
                productId,
                productName,
                state,
                diagnosis,
                priority,
                actions,
                explanation
        );
    }

    private MarketingState determineState(ProductMetrics metrics) {
        if (metrics.sales7d() > 5 && metrics.conversionRate() > 0.30) {
            return MarketingState.RENTABLE;
        }
        if (metrics.sales7d() >= 1 && metrics.sales7d() <= 4) {
            return MarketingState.BAJA_TRACCION;
        }
        if (metrics.sales14d() == 0 && metrics.interactions() > 5) {
            return MarketingState.ALERTA_COMERCIAL;
        }
        if (metrics.sales30d() == 0 && metrics.interactions() == 0) {
            return MarketingState.PAUSADO;
        }
        return MarketingState.DESCONOCIDO;
    }

    private MarketingDiagnosis determineDiagnosis(ProductMetrics metrics) {
        if (metrics.interactions() > 15 && metrics.sales14d() == 0) {
            return MarketingDiagnosis.PROBLEMA_CONFIANZA;
        }
        if (metrics.interactions() <= 10) {
            return MarketingDiagnosis.PROBLEMA_VISIBILIDAD;
        }
        if (metrics.conversionRate() < 0.10 && metrics.interactions() > 10) {
            return MarketingDiagnosis.PROBLEMA_PERCEPCION;
        }
        return MarketingDiagnosis.PROBLEMA_COMPLEJIDAD;
    }

    private MarketingPriority determinePriority(ProductMetrics metrics) {
        if (metrics.margin().doubleValue() > 50.0) {
            return MarketingPriority.ALTA;
        }
        if (metrics.interactions() > 20) {
            return MarketingPriority.MEDIA;
        }
        return MarketingPriority.BAJA;
    }

    private List<String> getActionsForDiagnosis(MarketingDiagnosis diagnosis) {
        return switch (diagnosis) {
            case PROBLEMA_VISIBILIDAD -> List.of("Publicacion educativa", "Historias destacadas", "Difusion segmentada");
            case PROBLEMA_CONFIANZA -> List.of("Recopilar testimonios", "Subir evidencias de activacion", "Resaltar garantia");
            case PROBLEMA_PERCEPCION -> List.of("Bundle con otro producto", "Bono temporal", "Comparativa de valor");
            case PROBLEMA_COMPLEJIDAD -> List.of("Mini guia de uso", "Video demo corto", "Post 'Como funciona'");
            default -> List.of();
        };
    }
}
