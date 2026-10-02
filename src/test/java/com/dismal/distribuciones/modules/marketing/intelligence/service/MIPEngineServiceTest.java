package com.dismal.distribuciones.modules.marketing.intelligence.service;

import com.dismal.distribuciones.modules.marketing.intelligence.domain.MarketingDiagnosis;
import com.dismal.distribuciones.modules.marketing.intelligence.domain.MarketingPriority;
import com.dismal.distribuciones.modules.marketing.intelligence.domain.MarketingState;
import com.dismal.distribuciones.modules.marketing.intelligence.dto.MarketingAnalysisResult;
import com.dismal.distribuciones.modules.marketing.intelligence.dto.ProductMetrics;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MIPEngineServiceTest {

    private final MIPEngineService engine = new MIPEngineService();

    @Test
    void shouldIdentifyRentableProduct() {
        ProductMetrics metrics = new ProductMetrics(10L, 20L, 40L, 50L, 0.8, new BigDecimal("100"), null);
        MarketingAnalysisResult result = engine.analyzeProduct(UUID.randomUUID(), "Test", metrics);

        assertEquals(MarketingState.RENTABLE, result.state());
        assertEquals(MarketingPriority.NINGUNA, result.priority());
    }

    @Test
    void shouldIdentifyCommercialAlertForVisibility() {
        ProductMetrics metrics = new ProductMetrics(0L, 0L, 0L, 6L, 0.0, new BigDecimal("10"), null);
        MarketingAnalysisResult result = engine.analyzeProduct(UUID.randomUUID(), "Alerta", metrics);

        assertEquals(MarketingState.ALERTA_COMERCIAL, result.state());
        assertEquals(MarketingDiagnosis.PROBLEMA_VISIBILIDAD, result.diagnosis());
        assertTrue(result.suggestedActions().contains("Publicacion educativa"));
    }

    @Test
    void shouldIdentifyCommercialAlertForConfidence() {
        ProductMetrics metrics = new ProductMetrics(0L, 0L, 0L, 20L, 0.0, new BigDecimal("10"), null);
        MarketingAnalysisResult result = engine.analyzeProduct(UUID.randomUUID(), "Confianza", metrics);

        assertEquals(MarketingState.ALERTA_COMERCIAL, result.state());
        assertEquals(MarketingDiagnosis.PROBLEMA_CONFIANZA, result.diagnosis());
        assertTrue(result.suggestedActions().contains("Recopilar testimonios"));
    }
}
