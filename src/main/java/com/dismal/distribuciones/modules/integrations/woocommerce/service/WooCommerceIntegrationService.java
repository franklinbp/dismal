package com.dismal.distribuciones.modules.integrations.woocommerce.service;

import com.dismal.distribuciones.exception.BadRequestException;
import com.dismal.distribuciones.modules.integrations.woocommerce.domain.WooOrderSync;
import com.dismal.distribuciones.modules.integrations.woocommerce.dto.WooCustomerSyncItemResponse;
import com.dismal.distribuciones.modules.integrations.woocommerce.dto.WooCustomerSyncPageResponse;
import com.dismal.distribuciones.modules.integrations.woocommerce.dto.WooDeliveredLicenseResponse;
import com.dismal.distribuciones.modules.integrations.woocommerce.dto.WooOrderPaidRequest;
import com.dismal.distribuciones.modules.integrations.woocommerce.dto.WooOrderPaidResponse;
import com.dismal.distribuciones.modules.integrations.woocommerce.repository.WooOrderSyncRepository;
import com.dismal.distribuciones.modules.sales.domain.PaymentMethod;
import com.dismal.distribuciones.modules.sales.domain.SaleType;
import com.dismal.distribuciones.modules.sales.dto.CreateSaleRequest;
import com.dismal.distribuciones.modules.sales.dto.PaymentRequest;
import com.dismal.distribuciones.modules.sales.dto.SaleItemRequest;
import com.dismal.distribuciones.modules.sales.dto.SaleResponse;
import com.dismal.distribuciones.modules.sales.service.PaymentService;
import com.dismal.distribuciones.modules.sales.service.SalesService;
import com.dismal.distribuciones.modules.security.domain.CustomerType;
import com.dismal.distribuciones.modules.security.domain.Role;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.repository.UserRepository;
import com.dismal.distribuciones.modules.security.util.PhoneNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WooCommerceIntegrationService {

    private final WooOrderSyncRepository wooOrderSyncRepository;
    private final UserRepository userRepository;
    private final SalesService salesService;
    private final PaymentService paymentService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public WooCustomerSyncPageResponse listCustomersForWooCommerce(int page, int size, boolean enabledOnly) {
        Specification<User> spec = Specification.where((root, query, cb) -> root.get("role").in(List.of(Role.USER, Role.CUSTOMER)));
        if (enabledOnly) {
            spec = spec.and((root, query, cb) -> cb.isTrue(root.get("enabled")));
        }

        PageRequest pageRequest = PageRequest.of(
                Math.max(page, 0),
                Math.max(size, 1),
                Sort.by(Sort.Direction.ASC, "email")
        );
        Page<User> customers = userRepository.findAll(spec, pageRequest);
        return new WooCustomerSyncPageResponse(
                customers.getContent().stream().map(this::toWooCustomerSyncItem).toList(),
                customers.getTotalElements(),
                customers.getTotalPages(),
                customers.getSize(),
                customers.getNumber(),
                customers.isLast()
        );
    }

    @Transactional
    public WooOrderPaidResponse processPaidOrder(WooOrderPaidRequest request) {
        WooOrderSync existing = wooOrderSyncRepository.findByExternalOrderId(request.orderId()).orElse(null);
        if (existing != null) {
            return new WooOrderPaidResponse(
                    existing.getExternalOrderId(),
                    existing.getSaleId(),
                    existing.getStatus(),
                    true,
                    false,
                    request.email(),
                    null,
                    salesService.listSaleLicenses(existing.getSaleId()).stream()
                            .map(license -> new WooDeliveredLicenseResponse(
                                    license.id(),
                                    license.licenseId(),
                                    license.licenseKey(),
                                    license.softwareId(),
                                    license.softwareName(),
                                    license.assignedAt()
                            ))
                            .toList()
            );
        }

        validateTotals(request);

        CustomerResolution customerResolution = findOrCreateCustomer(request);
        User customer = customerResolution.user();
        List<SaleItemRequest> saleItems = request.items().stream()
                .map(item -> new SaleItemRequest(item.softwareId(), item.quantity(), item.unitPrice(), null))
                .toList();

        SaleType saleType = resolveSaleType(request);
        SaleResponse sale = salesService.createSale(new CreateSaleRequest(customer.getId(), saleType, saleItems));
        SaleResponse confirmed = salesService.confirmSale(sale.id());
        SaleResponse finalSale = confirmed;
        if (saleType == SaleType.CASH) {
            finalSale = paymentService.addPayment(new PaymentRequest(
                    confirmed.id(),
                    request.totalPaid(),
                    request.paymentMethod(),
                    request.paymentReference() != null && !request.paymentReference().isBlank()
                            ? request.paymentReference()
                            : "woo:" + request.orderNumber()
            ));
        }

        WooOrderSync sync = wooOrderSyncRepository.save(WooOrderSync.builder()
                .externalOrderId(request.orderId())
                .saleId(finalSale.id())
                .status(finalSale.status().name())
                .build());

        return new WooOrderPaidResponse(
                sync.getExternalOrderId(),
                sync.getSaleId(),
                sync.getStatus(),
                false,
                customerResolution.created(),
                customer.getEmail(),
                customerResolution.generatedPassword(),
                salesService.listSaleLicenses(finalSale.id()).stream()
                        .map(license -> new WooDeliveredLicenseResponse(
                                license.id(),
                                license.licenseId(),
                                license.licenseKey(),
                                license.softwareId(),
                                license.softwareName(),
                                license.assignedAt()
                        ))
                        .toList()
        );
    }

    private void validateTotals(WooOrderPaidRequest request) {
        BigDecimal expected = request.items().stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (expected.compareTo(request.totalPaid()) != 0) {
            throw new BadRequestException("total_paid does not match the sum of item subtotals.");
        }
    }

    private SaleType resolveSaleType(WooOrderPaidRequest request) {
        if (request.saleType() == SaleType.CREDIT || request.paymentMethod() == PaymentMethod.CREDIT) {
            return SaleType.CREDIT;
        }
        return SaleType.CASH;
    }

    private CustomerResolution findOrCreateCustomer(WooOrderPaidRequest request) {
        User existing = userRepository.findByEmail(request.email()).orElse(null);
        if (existing != null) {
            if (!existing.isEnabled()) {
                throw new BadRequestException("Customer account is disabled.");
            }
            if (request.customerType() != null) {
                existing.setCustomerType(request.customerType());
            } else if (existing.getCustomerType() == null) {
                existing.setCustomerType(CustomerType.FINAL);
            }
            if (request.phone() != null && !request.phone().isBlank()) {
                existing.setPhone(PhoneNormalizer.normalize(request.phone()));
            }
            if (request.taxId() != null && !request.taxId().isBlank()) {
                existing.setTaxId(request.taxId());
            }
            if (existing.getBillingEmail() == null || existing.getBillingEmail().isBlank()) {
                existing.setBillingEmail(request.email());
            }
            return new CustomerResolution(userRepository.save(existing), false, null);
        }

        String generatedPassword = "Woo" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        User created = userRepository.save(User.builder()
                .firstname(request.firstName())
                .lastname(request.lastName())
                .email(request.email())
                .phone(request.phone() != null && !request.phone().isBlank() ? PhoneNormalizer.normalize(request.phone()) : null)
                .taxId(request.taxId())
                .billingEmail(request.email())
                .password(passwordEncoder.encode(generatedPassword))
                .role(Role.USER)
                .customerType(request.customerType() != null ? request.customerType() : CustomerType.FINAL)
                .enabled(true)
                .build());
        return new CustomerResolution(created, true, generatedPassword);
    }

    private WooCustomerSyncItemResponse toWooCustomerSyncItem(User user) {
        return new WooCustomerSyncItemResponse(
                user.getId(),
                user.getFirstname(),
                user.getLastname(),
                user.getPhone(),
                user.getEmail(),
                user.getTaxId(),
                user.getBillingEmail(),
                user.getCustomerType() != null ? user.getCustomerType() : CustomerType.FINAL,
                user.isHasCredit(),
                user.getCreditLimit(),
                user.getCreditUsed(),
                user.getCreditDays(),
                user.isEnabled()
        );
    }

    private record CustomerResolution(User user, boolean created, String generatedPassword) {}
}
