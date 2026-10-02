package com.dismal.distribuciones.modules.store.service;

import com.dismal.distribuciones.modules.store.domain.Software;
import com.dismal.distribuciones.modules.store.repository.SoftwareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductAnalyticsService {

    private final SoftwareRepository softwareRepository;

    /**
     * Finds products that have available stock but have not had any sales in the last 30 days.
     * This can be used by the marketing module to create targeted campaigns for "cold stock".
     * @return A list of "cold" software products that are in stock.
     */
    @Transactional(readOnly = true)
    public List<Software> findProductsWithoutRecentSales() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return softwareRepository.findInStockSoftwareWithoutRecentSales(thirtyDaysAgo);
    }
}
