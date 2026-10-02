package com.dismal.distribuciones.modules.marketing.intelligence.service;

import com.dismal.distribuciones.modules.marketing.intelligence.dto.MarketingAnalysisResult;
import com.dismal.distribuciones.modules.marketing.intelligence.dto.ProductMetrics;
import com.dismal.distribuciones.modules.marketing.intelligence.repository.ProductInteractionRepository;
import com.dismal.distribuciones.modules.sales.repository.OrderRepository;
import com.dismal.distribuciones.modules.sales.repository.SaleItemRepository;
import com.dismal.distribuciones.modules.store.domain.Software;
import com.dismal.distribuciones.modules.store.repository.LicenseRepository;
import com.dismal.distribuciones.modules.store.repository.SoftwareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarketingIntelligenceService {

    private final SoftwareRepository softwareRepository;
    private final OrderRepository orderRepository;
    private final SaleItemRepository saleItemRepository;
    private final LicenseRepository licenseRepository;
    private final ProductInteractionRepository interactionRepository;
    private final MIPEngineService mipEngineService;

    public List<MarketingAnalysisResult> analyzeAllProducts() {
        return softwareRepository.findAll().stream()
                .map(this::analyzeProduct)
                .collect(Collectors.toList());
    }

    public MarketingAnalysisResult analyzeProduct(Software software) {
        LocalDateTime now = LocalDateTime.now();
        
        long sales7d = realSalesUnitsSince(software.getId(), now.minusDays(7));
        long sales14d = realSalesUnitsSince(software.getId(), now.minusDays(14));
        long sales30d = realSalesUnitsSince(software.getId(), now.minusDays(30));
        
        long interactions30d = interactionRepository.countBySoftwareIdAndCreatedAtAfter(software.getId(), now.minusDays(30));
        
        double conversionRate = interactions30d == 0 ? 0 : (double) sales30d / interactions30d;
        
        BigDecimal averageActivationCost = averageActivationCost(software.getId());
        BigDecimal margin = software.getPrice().subtract(safe(averageActivationCost));

        ProductMetrics metrics = new ProductMetrics(
                sales7d,
                sales14d,
                sales30d,
                interactions30d,
                conversionRate,
                margin,
                toLocalDate(saleItemRepository.findLastSaleAtBySoftware(software.getId()))
        );

        return mipEngineService.analyzeProduct(software.getId(), software.getName(), metrics);
    }

    private long realSalesUnitsSince(UUID softwareId, LocalDateTime start) {
        long realUnits = saleItemRepository.sumUnitsBySoftwareSince(softwareId, start);
        if (realUnits > 0) {
            return realUnits;
        }
        return orderRepository.countByPurchasedSoftwareIdAndPurchaseDateAfter(softwareId, start);
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal averageActivationCost(UUID softwareId) {
        var licenses = licenseRepository.findBySoftwareId(softwareId);
        BigDecimal totalCost = BigDecimal.ZERO;
        int totalActivations = 0;
        for (var license : licenses) {
            totalCost = totalCost.add(safe(license.getPurchasePrice()));
            totalActivations += license.getMaxActivations() != null ? license.getMaxActivations() : 0;
        }
        if (totalActivations <= 0) {
            return BigDecimal.ZERO;
        }
        return totalCost.divide(BigDecimal.valueOf(totalActivations), 4, RoundingMode.HALF_UP);
    }

    private LocalDate toLocalDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toLocalDate() : null;
    }

    public void recordInteraction(UUID softwareId, String type) {
        Software software = softwareRepository.findById(softwareId)
                .orElseThrow(() -> new com.dismal.distribuciones.exception.ResourceNotFoundException("Software not found"));
        
        com.dismal.distribuciones.modules.marketing.intelligence.domain.ProductInteraction interaction = 
            com.dismal.distribuciones.modules.marketing.intelligence.domain.ProductInteraction.builder()
                .software(software)
                .type(type)
                .build();
        
        interactionRepository.save(interaction);
    }
}
