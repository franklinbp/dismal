package com.dismal.distribuciones.modules.sales.service;

import com.dismal.distribuciones.exception.ResourceNotFoundException;
import com.dismal.distribuciones.modules.sales.domain.PaymentAccount;
import com.dismal.distribuciones.modules.sales.domain.PaymentAccountType;
import com.dismal.distribuciones.modules.sales.dto.PaymentAccountRequest;
import com.dismal.distribuciones.modules.sales.dto.PaymentAccountResponse;
import com.dismal.distribuciones.modules.sales.dto.PaymentAccountMovementPageResponse;
import com.dismal.distribuciones.modules.sales.dto.PaymentAccountMovementResponse;
import com.dismal.distribuciones.modules.sales.dto.PaymentAccountUsageResponse;
import com.dismal.distribuciones.modules.sales.repository.PaymentAccountRepository;
import com.dismal.distribuciones.modules.sales.repository.PaymentRepository;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;
import com.dismal.distribuciones.modules.storefront.dto.StorefrontPaymentAccountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentAccountService {

    private final PaymentAccountRepository paymentAccountRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public List<PaymentAccountResponse> listAll() {
        return paymentAccountRepository.findAllByOrderByActiveDescDefaultAccountDescNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentAccountResponse> listActive() {
        return paymentAccountRepository.findByActiveTrueOrderByDefaultAccountDescNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentAccountUsageResponse> listUsage(boolean includeInactive) {
        Map<UUID, AccountUsage> usageByAccount = new HashMap<>();
        for (Object[] row : paymentRepository.summarizeByPaymentAccount()) {
            usageByAccount.put(
                    (UUID) row[0],
                    new AccountUsage(
                            (BigDecimal) row[1],
                            ((Number) row[2]).longValue(),
                            (LocalDateTime) row[3]
                    )
            );
        }
        List<PaymentAccount> accounts = includeInactive
                ? paymentAccountRepository.findAllByOrderByActiveDescDefaultAccountDescNameAsc()
                : paymentAccountRepository.findByActiveTrueOrderByDefaultAccountDescNameAsc();
        return accounts.stream()
                .map(account -> {
                    AccountUsage usage = usageByAccount.getOrDefault(
                            account.getId(),
                            new AccountUsage(BigDecimal.ZERO, 0, null)
                    );
                    return new PaymentAccountUsageResponse(
                            toResponse(account),
                            usage.recordedIncome(),
                            usage.paymentCount(),
                            usage.lastMovementAt()
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentAccountMovementPageResponse listMovements(UUID accountId, int page, int size) {
        if (accountId != null && !paymentAccountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Payment account not found with ID: " + accountId);
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        var movements = paymentRepository.findAccountMovements(
                accountId,
                PageRequest.of(safePage, safeSize)
        );
        var content = movements.getContent().stream()
                .map(payment -> {
                    var client = payment.getSale().getClient();
                    String firstName = client.getFirstname() != null ? client.getFirstname().trim() : "";
                    String lastName = client.getLastname() != null ? client.getLastname().trim() : "";
                    String clientName = (firstName + " " + lastName).trim();
                    return new PaymentAccountMovementResponse(
                            payment.getId(),
                            payment.getPaymentAccount().getId(),
                            payment.getPaymentAccount().getName(),
                            payment.getSale().getId(),
                            clientName,
                            client.getEmail(),
                            payment.getAmount(),
                            payment.getMethod(),
                            payment.getReference(),
                            payment.getCreatedAt()
                    );
                })
                .toList();
        return new PaymentAccountMovementPageResponse(
                content,
                safePage,
                safeSize,
                movements.getTotalElements(),
                movements.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public List<StorefrontPaymentAccountResponse> listStorefrontAccounts(StorefrontCountry country) {
        return paymentAccountRepository.findByActiveTrueAndPublicForStorefrontTrueOrderByDefaultAccountDescNameAsc()
                .stream()
                .filter(account -> country.name().equalsIgnoreCase(account.getCountryCode()))
                .filter(account -> country.currency().equalsIgnoreCase(account.getCurrency()))
                .filter(account -> account.getType() == PaymentAccountType.BANK)
                .map(account -> new StorefrontPaymentAccountResponse(
                        account.getId(),
                        account.getName(),
                        account.getCountryCode(),
                        account.getCurrency(),
                        account.getBankName(),
                        account.getAccountHolder(),
                        account.getAccountNumber(),
                        account.getAccountType(),
                        account.getTaxId()
                ))
                .toList();
    }

    @Transactional
    public PaymentAccountResponse create(PaymentAccountRequest request) {
        PaymentAccount account = PaymentAccount.builder()
                .name(cleanName(request.name()))
                .type(request.type() != null ? request.type() : PaymentAccountType.CASH)
                .currency(cleanCurrency(request.currency()))
                .active(request.active() == null || request.active())
                .defaultAccount(Boolean.TRUE.equals(request.defaultAccount()))
                .countryCode(cleanCountry(request.countryCode()))
                .publicForStorefront(Boolean.TRUE.equals(request.publicForStorefront()))
                .bankName(cleanOptional(request.bankName()))
                .accountHolder(cleanOptional(request.accountHolder()))
                .accountNumber(cleanOptional(request.accountNumber()))
                .accountType(cleanOptional(request.accountType()))
                .taxId(cleanOptional(request.taxId()))
                .build();
        validateStorefrontAccount(account);
        account = paymentAccountRepository.save(account);
        ensureSingleDefault(account);
        return toResponse(account);
    }

    @Transactional
    public PaymentAccountResponse update(UUID id, PaymentAccountRequest request) {
        PaymentAccount account = paymentAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment account not found with ID: " + id));
        account.setName(cleanName(request.name()));
        account.setType(request.type() != null ? request.type() : account.getType());
        account.setCurrency(cleanCurrency(request.currency()));
        account.setActive(request.active() == null || request.active());
        account.setDefaultAccount(Boolean.TRUE.equals(request.defaultAccount()));
        account.setCountryCode(cleanCountry(request.countryCode()));
        account.setPublicForStorefront(Boolean.TRUE.equals(request.publicForStorefront()));
        account.setBankName(cleanOptional(request.bankName()));
        account.setAccountHolder(cleanOptional(request.accountHolder()));
        account.setAccountNumber(cleanOptional(request.accountNumber()));
        account.setAccountType(cleanOptional(request.accountType()));
        account.setTaxId(cleanOptional(request.taxId()));
        validateStorefrontAccount(account);
        account = paymentAccountRepository.save(account);
        ensureSingleDefault(account);
        return toResponse(account);
    }

    @Transactional
    public PaymentAccount resolveForPayment(UUID paymentAccountId) {
        if (paymentAccountId != null) {
            PaymentAccount account = paymentAccountRepository.findById(paymentAccountId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment account not found with ID: " + paymentAccountId));
            if (!account.isActive()) {
                throw new IllegalArgumentException("Payment account is inactive");
            }
            return account;
        }

        return paymentAccountRepository.findFirstByDefaultAccountTrueAndActiveTrueOrderByNameAsc()
                .or(() -> paymentAccountRepository.findFirstByTypeAndActiveTrueOrderByDefaultAccountDescNameAsc(PaymentAccountType.CASH))
                .orElseGet(() -> paymentAccountRepository.save(PaymentAccount.builder()
                        .name("Caja principal")
                        .type(PaymentAccountType.CASH)
                        .currency("USD")
                        .active(true)
                        .defaultAccount(true)
                        .build()));
    }

    @Transactional(readOnly = true)
    public PaymentAccount resolveStorefrontAccount(
            UUID paymentAccountId,
            StorefrontCountry country
    ) {
        PaymentAccount account = paymentAccountRepository.findById(paymentAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta bancaria no encontrada."));
        if (!account.isActive() || !account.isPublicForStorefront() || account.getType() != PaymentAccountType.BANK) {
            throw new IllegalArgumentException("La cuenta bancaria no esta disponible para compras web.");
        }
        if (!country.name().equalsIgnoreCase(account.getCountryCode()) ||
                !country.currency().equalsIgnoreCase(account.getCurrency())) {
            throw new IllegalArgumentException("La cuenta bancaria no corresponde al pais de la orden.");
        }
        return account;
    }

    public PaymentAccountResponse toResponse(PaymentAccount account) {
        return new PaymentAccountResponse(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getCurrency(),
                account.isActive(),
                account.isDefaultAccount(),
                account.getCountryCode(),
                account.isPublicForStorefront(),
                account.getBankName(),
                account.getAccountHolder(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getTaxId()
        );
    }

    private void validateStorefrontAccount(PaymentAccount account) {
        if (!account.isPublicForStorefront()) {
            return;
        }
        if (account.getType() != PaymentAccountType.BANK) {
            throw new IllegalArgumentException("Una cuenta publica de tienda debe ser bancaria.");
        }
        if (account.getCountryCode() == null || account.getBankName() == null ||
                account.getAccountHolder() == null || account.getAccountNumber() == null) {
            throw new IllegalArgumentException("Completa pais, banco, titular y numero de cuenta.");
        }
    }

    private void ensureSingleDefault(PaymentAccount account) {
        if (!account.isDefaultAccount()) {
            return;
        }
        paymentAccountRepository.findAllByOrderByActiveDescDefaultAccountDescNameAsc().stream()
                .filter(candidate -> candidate.isDefaultAccount() && !candidate.getId().equals(account.getId()))
                .forEach(candidate -> {
                    candidate.setDefaultAccount(false);
                    paymentAccountRepository.save(candidate);
                });
    }

    private String cleanName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Payment account name is required");
        }
        return name.trim();
    }

    private String cleanCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "USD";
        }
        return currency.trim().toUpperCase();
    }

    private String cleanCountry(String country) {
        if (country == null || country.isBlank()) {
            return null;
        }
        String normalized = country.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals("EC") && !normalized.equals("PE")) {
            throw new IllegalArgumentException("Country must be EC or PE");
        }
        return normalized;
    }

    private String cleanOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record AccountUsage(BigDecimal recordedIncome, long paymentCount, LocalDateTime lastMovementAt) {}
}
