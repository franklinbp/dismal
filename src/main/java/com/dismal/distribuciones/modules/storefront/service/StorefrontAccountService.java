package com.dismal.distribuciones.modules.storefront.service;

import com.dismal.distribuciones.exception.BadRequestException;
import com.dismal.distribuciones.exception.ResourceNotFoundException;
import com.dismal.distribuciones.modules.sales.service.SalesService;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.repository.UserRepository;
import com.dismal.distribuciones.modules.storefront.dto.StorefrontAccountOrderResponse;
import com.dismal.distribuciones.modules.storefront.dto.StorefrontLicenseResponse;
import com.dismal.distribuciones.modules.storefront.dto.StorefrontOrderPageResponse;
import com.dismal.distribuciones.modules.storefront.dto.StorefrontOrderResponse;
import com.dismal.distribuciones.modules.storefront.repository.StorefrontOrderRepository;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontOrderStatus;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class StorefrontAccountService {

    private static final int MAX_PAGE_SIZE = 50;

    private final UserRepository userRepository;
    private final StorefrontOrderRepository storefrontOrderRepository;
    private final StorefrontOrderService storefrontOrderService;
    private final SalesService salesService;
    private final com.dismal.distribuciones.modules.store.service.PhysicalInventoryService physicalInventoryService;

    @Transactional(readOnly = true)
    public StorefrontOrderPageResponse listOrders(
            String authenticatedEmail,
            StorefrontCountry country,
            int page,
            int size
    ) {
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            throw new BadRequestException("Autenticacion requerida.");
        }
        User customer = userRepository.findByEmail(authenticatedEmail.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        var orders = storefrontOrderRepository.findByCustomerIdAndCountryOrderByCreatedAtDesc(
                customer.getId(),
                country != null ? country : StorefrontCountry.EC,
                PageRequest.of(safePage, safeSize)
        );
        var content = orders.getContent().stream()
                .map(order -> new StorefrontAccountOrderResponse(
                        storefrontOrderService.toResponse(order),
                        order.getSaleId() == null || !customer.isEmailVerified()
                                ? java.util.List.<StorefrontLicenseResponse>of()
                                : salesService.listSaleLicenses(order.getSaleId()).stream()
                                        .map(license -> new StorefrontLicenseResponse(
                                                license.licenseId(),
                                                license.softwareId(),
                                                license.softwareName(),
                                                license.licenseKey(),
                                                license.assignedAt()
                                        ))
                                        .toList()
                ))
                .toList();
        return new StorefrontOrderPageResponse(
                content,
                safePage,
                safeSize,
                orders.getTotalElements(),
                orders.getTotalPages()
        );
    }

    @Transactional
    public StorefrontOrderResponse cancelOrder(
            String authenticatedEmail,
            String orderNumber
    ) {
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            throw new BadRequestException("Autenticacion requerida.");
        }
        User customer = userRepository.findByEmail(authenticatedEmail.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
        var order = storefrontOrderRepository.findByOrderNumberForUpdate(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado."));

        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new ResourceNotFoundException("Pedido no encontrado.");
        }
        if (order.getStatus() == StorefrontOrderStatus.CANCELLED) {
            return storefrontOrderService.toResponse(order);
        }
        if (order.getStatus() != StorefrontOrderStatus.PENDING_PAYMENT
                || order.getSaleId() != null
                || order.getPaymentClaimedAt() != null) {
            throw new BadRequestException("Solo puedes cancelar pedidos pendientes que aun no tienen un pago reportado.");
        }

        order.setStatus(StorefrontOrderStatus.CANCELLED);
        physicalInventoryService.release(order);
        return storefrontOrderService.toResponse(storefrontOrderRepository.save(order));
    }
}
