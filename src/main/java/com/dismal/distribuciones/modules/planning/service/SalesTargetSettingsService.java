package com.dismal.distribuciones.modules.planning.service;

import com.dismal.distribuciones.modules.planning.domain.SalesTargetSettings;
import com.dismal.distribuciones.modules.planning.dto.SalesTargetSettingsRequest;
import com.dismal.distribuciones.modules.planning.dto.SalesTargetSettingsResponse;
import com.dismal.distribuciones.modules.planning.repository.SalesTargetSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class SalesTargetSettingsService {

    private static final BigDecimal DEFAULT_FIXED_COST = new BigDecimal("300");

    private final SalesTargetSettingsRepository repository;

    @Transactional(readOnly = true)
    public SalesTargetSettingsResponse getSettings() {
        SalesTargetSettings settings = repository.findTopByOrderByIdAsc()
                .orElseGet(() -> SalesTargetSettings.builder()
                        .fixedCostGlobal(DEFAULT_FIXED_COST)
                        .build());
        return new SalesTargetSettingsResponse(settings.getFixedCostGlobal());
    }

    @Transactional
    public SalesTargetSettingsResponse updateSettings(SalesTargetSettingsRequest request) {
        BigDecimal fixedCost = request != null && request.fixedCostGlobal() != null
                ? request.fixedCostGlobal()
                : DEFAULT_FIXED_COST;
        if (fixedCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("fixedCostGlobal must be >= 0");
        }
        SalesTargetSettings settings = repository.findTopByOrderByIdAsc()
                .orElseGet(() -> SalesTargetSettings.builder().build());
        settings.setFixedCostGlobal(fixedCost);
        SalesTargetSettings saved = repository.save(settings);
        return new SalesTargetSettingsResponse(saved.getFixedCostGlobal());
    }

    @Transactional(readOnly = true)
    public BigDecimal getFixedCostGlobal() {
        return repository.findTopByOrderByIdAsc()
                .map(SalesTargetSettings::getFixedCostGlobal)
                .orElse(DEFAULT_FIXED_COST);
    }
}
