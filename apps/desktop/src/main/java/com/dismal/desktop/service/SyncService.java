package com.dismal.desktop.service;

import com.dismal.desktop.ApiClient;
import com.dismal.desktop.License;
import com.dismal.desktop.PageResponse;
import com.dismal.desktop.Product;
import com.dismal.desktop.SalesTarget;
import com.dismal.desktop.local.domain.LocalLicense;
import com.dismal.desktop.local.domain.LocalProduct;
import com.dismal.desktop.local.domain.LocalSalesTarget;
import com.dismal.desktop.local.repository.LocalLicenseRepository;
import com.dismal.desktop.local.repository.LocalProductRepository;
import com.dismal.desktop.local.repository.LocalSalesTargetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {

    private final LocalProductRepository localProductRepository;
    private final LocalLicenseRepository localLicenseRepository;
    private final LocalSalesTargetRepository localSalesTargetRepository;

    private final java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(SyncService.class);
    private static final String LAST_SYNC_KEY = "last_sync_timestamp";

    public void syncAll(ApiClient apiClient) throws IOException, InterruptedException {
        long start = System.currentTimeMillis();
        String lastSync = prefs.get(LAST_SYNC_KEY, null);
        log.info("Starting synchronization... Last sync: {}", lastSync != null ? lastSync : "Never");

        try {
            pullProducts(apiClient);
            pullLicenses(apiClient);
            pullSalesTargets(apiClient);
            
            // Update timestamp only on success
            prefs.put(LAST_SYNC_KEY, java.time.Instant.now().toString());
            log.info("Synchronization completed in {} ms.", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("Sync failed", e);
            throw e; // Propagate to UI
        }
    }

    @Transactional
    public void pullProducts(ApiClient apiClient) throws IOException, InterruptedException {
        List<Product> cloudProducts = apiClient.listProducts();
        log.info("Fetched {} products from cloud.", cloudProducts.size());

        List<LocalProduct> localEntities = cloudProducts.stream()
                .map(p -> new LocalProduct(
                        p.id(),
                        p.name(),
                        BigDecimal.valueOf(p.price()),
                        p.platform(),
                        p.description()
                ))
                .collect(Collectors.toList());

        localProductRepository.saveAll(localEntities);
        log.info("Saved products to local database.");
    }

    @Transactional
    public void pullLicenses(ApiClient apiClient) throws IOException, InterruptedException {
        // Fetch all licenses (pagination handling simplified for demo, getting first 1000)
        PageResponse<License> page = apiClient.listLicenses(null, null, 0, 1000);
        List<License> cloudLicenses = page.content();
        log.info("Fetched {} licenses from cloud.", cloudLicenses.size());

        List<LocalLicense> localEntities = cloudLicenses.stream()
                .map(l -> LocalLicense.builder()
                        .id(l.id())
                        .softwareId(l.softwareId())
                        .softwareName(l.softwareName())
                        .licenseKey(l.licenseKey())
                        .status(l.status())
                        .usedActivations(l.usedActivations())
                        .maxActivations(l.maxActivations())
                        .purchasePrice(BigDecimal.valueOf(l.purchasePrice()))
                        .build())
                .collect(Collectors.toList());

        localLicenseRepository.saveAll(localEntities);
        log.info("Saved licenses to local database.");
    }

    @Transactional
    public void pullSalesTargets(ApiClient apiClient) throws IOException, InterruptedException {
        List<SalesTarget> cloudTargets = apiClient.listSalesTargets();
        log.info("Fetched {} sales targets from cloud.", cloudTargets.size());

        List<LocalSalesTarget> localEntities = cloudTargets.stream()
                .map(t -> LocalSalesTarget.builder()
                        .id(t.id())
                        .softwareId(t.softwareId())
                        .softwareName(t.softwareName())
                        .metaUnits(t.metaUnits())
                        .unitsSoldCurrent(t.unitsSoldCurrent())
                        .salePrice(BigDecimal.valueOf(t.salePrice()))
                        .variableCost(BigDecimal.valueOf(t.variableCost()))
                        .fixedCostProduct(t.fixedCostProduct() != null ? BigDecimal.valueOf(t.fixedCostProduct()) : null)
                        .deadline(t.deadline())
                        .notes(t.notes())
                        .profitable(t.profitable())
                        .marginUnit(BigDecimal.valueOf(t.marginUnit()))
                        .expectedProfit(BigDecimal.valueOf(t.expectedProfit()))
                        .build())
                .collect(Collectors.toList());

        localSalesTargetRepository.saveAll(localEntities);
        log.info("Saved sales targets to local database.");
    }
}
