package com.dismal.distribuciones.modules.sales.service;

import com.dismal.distribuciones.modules.sales.domain.SaleNumberCounter;
import com.dismal.distribuciones.modules.sales.repository.SaleNumberCounterRepository;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SaleNumberService {

    public static final long ECUADOR_FIRST_NUMBER = 10_000L;
    public static final long PERU_FIRST_NUMBER = 20_000L;

    private final SaleNumberCounterRepository counterRepository;

    @Transactional
    public long nextNumber(StorefrontCountry country) {
        StorefrontCountry market = country != null ? country : StorefrontCountry.EC;
        SaleNumberCounter counter = counterRepository.findByCountryForUpdate(market)
                .orElseGet(() -> counterRepository.saveAndFlush(SaleNumberCounter.builder()
                        .country(market)
                        .nextNumber(firstNumber(market))
                        .build()));

        long next = Math.max(counter.getNextNumber(), firstNumber(market));
        counter.setNextNumber(Math.addExact(next, 1L));
        counterRepository.save(counter);
        return next;
    }

    public static String format(StorefrontCountry country, Long saleNumber) {
        if (saleNumber == null) {
            return null;
        }
        StorefrontCountry market = country != null ? country : StorefrontCountry.EC;
        return market.name() + "-" + saleNumber;
    }

    private long firstNumber(StorefrontCountry country) {
        return country == StorefrontCountry.PE ? PERU_FIRST_NUMBER : ECUADOR_FIRST_NUMBER;
    }
}
