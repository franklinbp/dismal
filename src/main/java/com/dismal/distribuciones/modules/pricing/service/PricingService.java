package com.dismal.distribuciones.modules.pricing.service;

import com.dismal.distribuciones.exception.ResourceNotFoundException;
import com.dismal.distribuciones.modules.pricing.domain.PriceList;
import com.dismal.distribuciones.modules.pricing.domain.PriceListItem;
import com.dismal.distribuciones.modules.pricing.domain.PriceListType;
import com.dismal.distribuciones.modules.pricing.dto.ProductPricingResponse;
import com.dismal.distribuciones.modules.pricing.repository.PriceListRepository;
import com.dismal.distribuciones.modules.pricing.repository.PriceListItemRepository;
import com.dismal.distribuciones.modules.security.domain.CustomerType;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.store.domain.Software;
import com.dismal.distribuciones.modules.store.repository.SoftwareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final PriceListRepository priceListRepository;
    private final PriceListItemRepository priceListItemRepository;
    private final SoftwareRepository softwareRepository;

    @Transactional(readOnly = true)
    public BigDecimal resolvePrice(Software software, User customer) {
        PriceListType priceListType = resolvePriceListType(customer);
        PriceListType fallbackType = priceListType.customerType() == CustomerType.DISTRIBUTOR
                ? PriceListType.from(priceListType.countryCode(), CustomerType.FINAL)
                : null;

        return priceListItemRepository.findPriceByTypeAndSoftwareId(priceListType, software.getId())
                .or(() -> fallbackType != null
                        ? priceListItemRepository.findPriceByTypeAndSoftwareId(fallbackType, software.getId())
                        : java.util.Optional.empty())
                .orElse(software.getPrice());
    }

    public PriceListType resolvePriceListType(User customer) {
        CustomerType customerType = customer != null && customer.getCustomerType() != null
                ? customer.getCustomerType()
                : CustomerType.FINAL;
        return PriceListType.from("EC", customerType);
    }

    @Transactional(readOnly = true)
    public ProductPricingResponse getProductPricing(java.util.UUID softwareId) {
        Software software = softwareRepository.findById(softwareId)
                .orElseThrow(() -> new ResourceNotFoundException("Software not found with ID: " + softwareId));

        BigDecimal ecFinalPrice = priceListItemRepository.findPriceByTypeAndSoftwareId(PriceListType.EC_FINAL, softwareId)
                .orElse(software.getPrice());
        BigDecimal ecDistributorPrice = priceListItemRepository.findPriceByTypeAndSoftwareId(PriceListType.EC_DISTRIBUTOR, softwareId)
                .orElse(ecFinalPrice);
        BigDecimal peFinalPrice = priceListItemRepository.findPriceByTypeAndSoftwareId(PriceListType.PE_FINAL, softwareId)
                .orElse(ecFinalPrice);
        BigDecimal peDistributorPrice = priceListItemRepository.findPriceByTypeAndSoftwareId(PriceListType.PE_DISTRIBUTOR, softwareId)
                .orElse(peFinalPrice);

        return new ProductPricingResponse(
                software.getId(),
                software.getName(),
                ecFinalPrice,
                ecDistributorPrice,
                peFinalPrice,
                peDistributorPrice
        );
    }

    @Transactional(readOnly = true)
    public BigDecimal resolvePrice(Software software, String countryCode, CustomerType customerType) {
        PriceListType priceListType = PriceListType.from(countryCode, customerType);
        PriceListType fallbackType = customerType == CustomerType.DISTRIBUTOR
                ? PriceListType.from(priceListType.countryCode(), CustomerType.FINAL)
                : null;

        return priceListItemRepository.findPriceByTypeAndSoftwareId(priceListType, software.getId())
                .or(() -> fallbackType != null
                        ? priceListItemRepository.findPriceByTypeAndSoftwareId(fallbackType, software.getId())
                        : java.util.Optional.empty())
                .orElse(software.getPrice());
    }

    @Transactional
    public void upsertPrice(java.util.UUID softwareId, PriceListType type, BigDecimal price) {
        Software software = softwareRepository.findById(softwareId)
                .orElseThrow(() -> new ResourceNotFoundException("Software not found with ID: " + softwareId));
        PriceList priceList = ensurePriceList(type);
        PriceListItem item = priceListItemRepository.findByPriceListIdAndSoftwareId(priceList.getId(), softwareId)
                .orElseGet(() -> PriceListItem.builder()
                        .priceList(priceList)
                        .software(software)
                        .build());
        item.setPrice(price);
        priceListItemRepository.save(item);

        if (type.isDefaultBasePrice()) {
            software.setPrice(price);
            softwareRepository.save(software);
        }
    }

    @Transactional
    public PriceList ensurePriceList(PriceListType type) {
        return priceListRepository.findByType(type)
                .orElseGet(() -> priceListRepository.save(PriceList.builder()
                        .type(type)
                        .name(type.displayName())
                        .enabled(true)
                        .build()));
    }
}
