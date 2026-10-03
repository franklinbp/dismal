package com.dismal.distribuciones.modules.store.service;

import com.dismal.distribuciones.exception.ConflictException;
import com.dismal.distribuciones.exception.ResourceNotFoundException;
import com.dismal.distribuciones.modules.pricing.domain.PriceListType;
import com.dismal.distribuciones.modules.pricing.service.PricingService;
import com.dismal.distribuciones.modules.sales.repository.SaleRepository;
import com.dismal.distribuciones.modules.sales.service.SalesService;
import com.dismal.distribuciones.modules.sales.repository.SaleFulfillmentRepository;
import com.dismal.distribuciones.modules.store.domain.License;
import com.dismal.distribuciones.modules.store.domain.LicenseStatus;
import com.dismal.distribuciones.modules.store.domain.Software;
import com.dismal.distribuciones.modules.store.dto.SoftwarePriceUpdateRequest;
import com.dismal.distribuciones.modules.store.repository.LicenseRepository;
import com.dismal.distribuciones.modules.store.repository.SoftwareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final SoftwareRepository softwareRepository;
    private final LicenseRepository licenseRepository;
    private final SaleFulfillmentRepository saleFulfillmentRepository;
    private final SaleRepository saleRepository;
    private final SalesService salesService;
    private final PricingService pricingService;

    @Transactional
    public Software saveSoftware(Software software) {
        Software savedSoftware = softwareRepository.save(software);
        syncDefaultPriceLists(savedSoftware);
        return savedSoftware;
    }

    @Transactional
    public Software saveSoftware(Software software,
                                 BigDecimal ecFinalPrice,
                                 BigDecimal ecDistributorPrice) {
        software.setPrice(ecFinalPrice);
        Software savedSoftware = softwareRepository.save(software);
        syncConfiguredPriceLists(savedSoftware, ecFinalPrice, ecDistributorPrice);
        return savedSoftware;
    }

    @Transactional
    public License addLicense(UUID softwareId, License license) {
        // Find the software product by its ID, or throw an exception if not found.
        Software software = softwareRepository.findById(softwareId)
                .orElseThrow(() -> new ResourceNotFoundException("Software not found with ID: " + softwareId));

        // Associate the license with the found software product.
        license.setSoftware(software);

        // Save the new license to the database.
        return licenseRepository.save(license);
    }

    @Transactional(readOnly = true)
    public List<Software> getAllSoftware() {
        return softwareRepository.findAll();
    }

    @Transactional
    public Software updateSoftware(UUID id, Software softwareDetails) {
        Software existingSoftware = softwareRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Software not found with ID: " + id));

        existingSoftware.setName(softwareDetails.getName());
        existingSoftware.setDescription(softwareDetails.getDescription());
        existingSoftware.setPrice(softwareDetails.getPrice());
        existingSoftware.setPlatform(softwareDetails.getPlatform());
        existingSoftware.setImageUrl(softwareDetails.getImageUrl());
        copyCommerceFields(existingSoftware, softwareDetails);

        Software updatedSoftware = softwareRepository.save(existingSoftware);
        syncPublicPriceList(updatedSoftware);
        return updatedSoftware;
    }

    @Transactional
    public void deleteSoftware(UUID id) {
        if (!softwareRepository.existsById(id)) {
            throw new ResourceNotFoundException("Software not found with ID: " + id);
        }
        long licenseCount = licenseRepository.countBySoftwareId(id);
        if (licenseCount > 0) {
            throw new ConflictException("No se puede eliminar el producto porque tiene licencias asociadas.");
        }
        softwareRepository.deleteById(id);
    }

    @Transactional
    public void forceDeleteSoftware(UUID id) {
        if (!softwareRepository.existsById(id)) {
            throw new ResourceNotFoundException("Software not found with ID: " + id);
        }
        var licenses = licenseRepository.findBySoftwareId(id);
        List<UUID> saleIds = saleRepository.findSaleIdsBySoftwareId(id);
        if (!saleIds.isEmpty()) {
            for (UUID saleId : saleIds) {
                salesService.cancelSale(saleId);
                salesService.cleanupSaleFinancials(saleId);
                saleFulfillmentRepository.deleteBySaleId(saleId);
            }
        }
        licenseRepository.deleteBySoftwareId(id);
        softwareRepository.deleteById(id);
    }

    @Transactional
    public Software updateSoftwarePrice(UUID id, SoftwarePriceUpdateRequest request) {
        Software existingSoftware = softwareRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Software not found with ID: " + id));
        existingSoftware.setPrice(request.price());
        Software updatedSoftware = softwareRepository.save(existingSoftware);
        syncPublicPriceList(updatedSoftware);
        return updatedSoftware;
    }

    @Transactional
    public Software updateSoftware(UUID id,
                                   Software softwareDetails,
                                   BigDecimal ecFinalPrice,
                                   BigDecimal ecDistributorPrice) {
        Software existingSoftware = softwareRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Software not found with ID: " + id));

        existingSoftware.setName(softwareDetails.getName());
        existingSoftware.setDescription(softwareDetails.getDescription());
        existingSoftware.setPrice(ecFinalPrice);
        existingSoftware.setPlatform(softwareDetails.getPlatform());
        existingSoftware.setImageUrl(softwareDetails.getImageUrl());
        copyCommerceFields(existingSoftware, softwareDetails);

        Software updatedSoftware = softwareRepository.save(existingSoftware);
        syncConfiguredPriceLists(updatedSoftware, ecFinalPrice, ecDistributorPrice);
        return updatedSoftware;
    }

    @Transactional
    public void deactivateLicense(UUID licenseId) {
        License license = licenseRepository.findById(licenseId)
                .orElseThrow(() -> new ResourceNotFoundException("License not found with ID: " + licenseId));
        license.setStatus(LicenseStatus.DAMAGED);
        licenseRepository.save(license);
    }

    private void syncDefaultPriceLists(Software software) {
        syncConfiguredPriceLists(software, software.getPrice(), software.getPrice());
    }

    private void syncPublicPriceList(Software software) {
        syncPriceList(software, PriceListType.EC_FINAL, software.getPrice());
    }

    private void syncConfiguredPriceLists(Software software,
                                          BigDecimal ecFinalPrice,
                                          BigDecimal ecDistributorPrice) {
        syncPriceList(software, PriceListType.EC_FINAL, ecFinalPrice);
        syncPriceList(software, PriceListType.EC_DISTRIBUTOR, ecDistributorPrice != null ? ecDistributorPrice : ecFinalPrice);
    }

    private void copyCommerceFields(Software target, Software source) {
        target.setSku(source.getSku());
        target.setBarcode(source.getBarcode());
        target.setBrand(source.getBrand());
        target.setPhysicalProduct(Boolean.TRUE.equals(source.getPhysicalProduct()));
        target.setStockQuantity(source.getStockQuantity() != null ? Math.max(0, source.getStockQuantity()) : 0);
        if (!Boolean.TRUE.equals(target.getPhysicalProduct())) {
            target.setReservedQuantity(0);
        }
    }

    private void syncPriceList(Software software, PriceListType type, java.math.BigDecimal price) {
        pricingService.upsertPrice(software.getId(), type, price != null ? price : java.math.BigDecimal.ZERO);
    }
}
