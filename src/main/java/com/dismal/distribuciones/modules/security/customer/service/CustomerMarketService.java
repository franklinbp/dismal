package com.dismal.distribuciones.modules.security.customer.service;

import com.dismal.distribuciones.exception.BadRequestException;
import com.dismal.distribuciones.exception.ResourceNotFoundException;
import com.dismal.distribuciones.modules.security.customer.domain.CustomerMarket;
import com.dismal.distribuciones.modules.security.customer.repository.CustomerMarketRepository;
import com.dismal.distribuciones.modules.security.domain.CustomerType;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.repository.UserRepository;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerMarketService {

    private final CustomerMarketRepository customerMarketRepository;
    private final UserRepository userRepository;

    @Transactional
    public CustomerMarket getOrCreate(User user, StorefrontCountry country) {
        return getOrCreate(user, country, null, null);
    }

    @Transactional
    public CustomerMarket getOrCreate(
            User user,
            StorefrontCountry country,
            String taxId,
            String billingEmail
    ) {
        requireIdentity(user);
        StorefrontCountry safeCountry = country != null ? country : StorefrontCountry.EC;
        CustomerMarket existing = customerMarketRepository.findByUserIdAndCountry(user.getId(), safeCountry)
                .orElse(null);
        if (existing != null) {
            boolean changed = applyMissingBillingData(existing, taxId, billingEmail);
            return changed ? customerMarketRepository.save(existing) : existing;
        }

        boolean firstMarket = !customerMarketRepository.existsByUserId(user.getId());
        CustomerMarket created = CustomerMarket.builder()
                .user(user)
                .country(safeCountry)
                .currency(safeCountry.currency())
                .customerType(firstMarket ? safeCustomerType(user.getCustomerType()) : CustomerType.FINAL)
                .taxId(clean(taxId) != null ? clean(taxId) : firstMarket ? clean(user.getTaxId()) : null)
                .billingEmail(clean(billingEmail) != null
                        ? clean(billingEmail)
                        : firstMarket ? defaultBillingEmail(user) : user.getEmail())
                .hasCredit(firstMarket && user.isHasCredit())
                .creditLimit(firstMarket ? safeMoney(user.getCreditLimit()) : BigDecimal.ZERO)
                .creditUsed(firstMarket ? safeMoney(user.getCreditUsed()) : BigDecimal.ZERO)
                .creditDays(firstMarket ? safeDays(user.getCreditDays()) : 0)
                .active(user.isEnabled())
                .primaryMarket(firstMarket)
                .build();
        try {
            CustomerMarket saved = customerMarketRepository.saveAndFlush(created);
            if (saved.isPrimaryMarket()) {
                syncLegacyFields(saved);
            }
            return saved;
        } catch (DataIntegrityViolationException ex) {
            return customerMarketRepository.findByUserIdAndCountry(user.getId(), safeCountry)
                    .orElseThrow(() -> ex);
        }
    }

    @Transactional(readOnly = true)
    public CustomerMarket find(User user, StorefrontCountry country) {
        if (user == null || user.getId() == null) {
            return null;
        }
        return customerMarketRepository.findByUserIdAndCountry(
                user.getId(), country != null ? country : StorefrontCountry.EC
        ).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<CustomerMarket> findAll(User user) {
        requireIdentity(user);
        return customerMarketRepository.findAllByUserIdOrderByCountryAsc(user.getId());
    }

    @Transactional
    public CustomerMarket resolveForSale(User user, StorefrontCountry requestedCountry) {
        requireIdentity(user);
        if (requestedCountry != null) {
            return requireActive(getOrCreate(user, requestedCountry));
        }
        CustomerMarket primary = customerMarketRepository.findFirstByUserIdAndPrimaryMarketTrue(user.getId())
                .orElse(null);
        if (primary != null) {
            return requireActive(primary);
        }
        List<CustomerMarket> markets = customerMarketRepository.findAllByUserIdOrderByCountryAsc(user.getId());
        if (!markets.isEmpty()) {
            return requireActive(markets.get(0));
        }
        return requireActive(getOrCreate(user, inferLegacyCountry(user)));
    }

    @Transactional(readOnly = true)
    public CustomerType resolveCustomerType(User user, StorefrontCountry country) {
        if (user == null || user.getId() == null) {
            return CustomerType.FINAL;
        }
        CustomerMarket market = find(user, country);
        if (market != null) {
            return market.isActive() ? safeCustomerType(market.getCustomerType()) : CustomerType.FINAL;
        }
        if (!customerMarketRepository.existsByUserId(user.getId())) {
            return safeCustomerType(user.getCustomerType());
        }
        return CustomerType.FINAL;
    }

    @Transactional
    public CustomerMarket updateCommercialProfile(
            User user,
            StorefrontCountry country,
            CustomerType customerType,
            String taxId,
            String billingEmail,
            Boolean hasCredit,
            BigDecimal creditLimit,
            Integer creditDays,
            Boolean active
    ) {
        CustomerMarket market = getOrCreate(user, country, taxId, billingEmail);
        if (customerType != null) {
            market.setCustomerType(customerType);
        }
        market.setTaxId(clean(taxId));
        market.setBillingEmail(clean(billingEmail) != null ? clean(billingEmail) : user.getEmail());
        if (hasCredit != null) {
            market.setHasCredit(hasCredit);
        }
        if (creditLimit != null) {
            if (creditLimit.signum() < 0) {
                throw new BadRequestException("El limite de credito no puede ser negativo.");
            }
            if (creditLimit.compareTo(safeMoney(market.getCreditUsed())) < 0) {
                throw new BadRequestException("El limite no puede ser menor al credito utilizado.");
            }
            market.setCreditLimit(creditLimit);
        }
        if (creditDays != null) {
            if (creditDays < 0) {
                throw new BadRequestException("Los dias de credito no pueden ser negativos.");
            }
            market.setCreditDays(creditDays);
        }
        if (active != null) {
            market.setActive(active);
        }
        CustomerMarket saved = customerMarketRepository.save(market);
        syncLegacyFields(saved);
        return saved;
    }

    @Transactional
    public CustomerMarket updateBillingProfile(
            User user,
            StorefrontCountry country,
            String taxId,
            String billingEmail
    ) {
        CustomerMarket market = getOrCreate(user, country);
        market.setTaxId(clean(taxId));
        market.setBillingEmail(clean(billingEmail) != null ? clean(billingEmail) : user.getEmail());
        CustomerMarket saved = customerMarketRepository.save(market);
        syncLegacyFields(saved);
        return saved;
    }

    @Transactional
    public CustomerMarket lock(CustomerMarket market) {
        if (market == null || market.getId() == null) {
            throw new ResourceNotFoundException("Cuenta comercial no encontrada.");
        }
        return customerMarketRepository.findByIdForUpdate(market.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta comercial no encontrada."));
    }

    @Transactional
    public CustomerMarket saveCredit(CustomerMarket market) {
        CustomerMarket saved = customerMarketRepository.save(market);
        syncLegacyFields(saved);
        return saved;
    }

    public CustomerMarket requireActive(CustomerMarket market) {
        if (market == null || !market.isActive()) {
            throw new BadRequestException("La cuenta comercial para este pais esta deshabilitada.");
        }
        return market;
    }

    private void syncLegacyFields(CustomerMarket market) {
        if (market == null || !market.isPrimaryMarket() || market.getUser() == null) {
            return;
        }
        User user = market.getUser();
        user.setCustomerType(safeCustomerType(market.getCustomerType()));
        user.setTaxId(market.getTaxId());
        user.setBillingEmail(market.getBillingEmail());
        user.setHasCredit(market.isHasCredit());
        user.setCreditLimit(safeMoney(market.getCreditLimit()));
        user.setCreditUsed(safeMoney(market.getCreditUsed()));
        user.setCreditDays(safeDays(market.getCreditDays()));
        userRepository.save(user);
    }

    private boolean applyMissingBillingData(CustomerMarket market, String taxId, String billingEmail) {
        boolean changed = false;
        String cleanTaxId = clean(taxId);
        if (cleanTaxId != null && (market.getTaxId() == null || market.getTaxId().isBlank())) {
            market.setTaxId(cleanTaxId);
            changed = true;
        }
        String cleanBillingEmail = clean(billingEmail);
        if (cleanBillingEmail != null && (market.getBillingEmail() == null || market.getBillingEmail().isBlank())) {
            market.setBillingEmail(cleanBillingEmail);
            changed = true;
        }
        return changed;
    }

    private StorefrontCountry inferLegacyCountry(User user) {
        String phone = user.getPhone() != null ? user.getPhone().replaceAll("[^0-9]", "") : "";
        return phone.startsWith("51") ? StorefrontCountry.PE : StorefrontCountry.EC;
    }

    private void requireIdentity(User user) {
        if (user == null || user.getId() == null) {
            throw new ResourceNotFoundException("Usuario no encontrado.");
        }
    }

    private String defaultBillingEmail(User user) {
        return clean(user.getBillingEmail()) != null ? clean(user.getBillingEmail()) : user.getEmail();
    }

    private CustomerType safeCustomerType(CustomerType value) {
        return value != null ? value : CustomerType.FINAL;
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private Integer safeDays(Integer value) {
        return value != null ? value : 0;
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
