package com.dismal.distribuciones.modules.storefront.service;

import com.dismal.distribuciones.exception.BadRequestException;
import com.dismal.distribuciones.exception.ResourceNotFoundException;
import com.dismal.distribuciones.modules.sales.domain.PaymentAccount;
import com.dismal.distribuciones.modules.sales.domain.PaymentMethod;
import com.dismal.distribuciones.modules.sales.service.PaymentAccountService;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCheckoutMethod;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontOrder;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontOrderStatus;
import com.dismal.distribuciones.modules.storefront.dto.StorefrontOrderResponse;
import com.dismal.distribuciones.modules.storefront.dto.StorefrontPaymentClaimRequest;
import com.dismal.distribuciones.modules.storefront.dto.StorefrontPaymentConfirmationRequest;
import com.dismal.distribuciones.modules.storefront.dto.StorefrontPaymentReviewRequest;
import com.dismal.distribuciones.modules.storefront.repository.StorefrontOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorefrontPaymentService {

    private final StorefrontOrderRepository storefrontOrderRepository;
    private final StorefrontOrderService storefrontOrderService;
    private final StorefrontFulfillmentService storefrontFulfillmentService;
    private final PaymentAccountService paymentAccountService;

    @Transactional
    public StorefrontOrderResponse submitPaymentClaim(
            String orderNumber,
            String authenticatedEmail,
            StorefrontPaymentClaimRequest request
    ) {
        StorefrontOrder order = findLockedOrder(orderNumber);
        validateOwner(order, authenticatedEmail);
        if (order.getCheckoutMethod() != StorefrontCheckoutMethod.BANK_TRANSFER) {
            throw new BadRequestException("Esta orden no utiliza transferencia bancaria.");
        }
        if (order.getSaleId() != null || order.getStatus() == StorefrontOrderStatus.PREPARING) {
            return storefrontOrderService.toResponse(order);
        }
        if (order.getStatus() != StorefrontOrderStatus.PENDING_PAYMENT &&
                order.getStatus() != StorefrontOrderStatus.PAYMENT_REVIEW) {
            throw new BadRequestException("La orden no admite un comprobante en su estado actual.");
        }

        PaymentAccount account = paymentAccountService.resolveStorefrontAccount(
                request.paymentAccountId(),
                order.getCountry()
        );
        String reference = cleanRequired(request.reference(), "Referencia bancaria requerida.")
                .toUpperCase(Locale.ROOT);
        storefrontOrderRepository.findByPaymentAccountIdAndPaymentClaimReference(account.getId(), reference)
                .filter(existing -> !existing.getId().equals(order.getId()))
                .ifPresent(existing -> {
                    throw new BadRequestException("La referencia bancaria ya fue registrada en otra orden.");
                });

        order.setPaymentAccountId(account.getId());
        order.setPaymentClaimBank(cleanRequired(request.sourceBank(), "Banco de origen requerido."));
        order.setPaymentClaimReference(reference);
        order.setPaymentClaimPayer(cleanRequired(request.payerName(), "Nombre del pagador requerido."));
        order.setPaymentClaimAmount(request.amount());
        order.setPaymentClaimedAt(LocalDateTime.now());
        order.setPaymentReviewNotes(null);
        order.setPaymentReviewedAt(null);
        order.setPaymentReviewedBy(null);
        order.setStatus(StorefrontOrderStatus.PAYMENT_REVIEW);
        return storefrontOrderService.toResponse(storefrontOrderRepository.save(order));
    }

    @Transactional
    public StorefrontOrderResponse reviewPayment(
            String orderNumber,
            StorefrontPaymentReviewRequest request,
            String reviewerEmail
    ) {
        if (request == null || request.approved() == null) {
            throw new BadRequestException("Decision de pago requerida.");
        }
        StorefrontOrder order = findLockedOrder(orderNumber);
        if (order.getSaleId() != null) {
            return storefrontOrderService.toResponse(order);
        }
        if (order.getStatus() != StorefrontOrderStatus.PAYMENT_REVIEW) {
            throw new BadRequestException("La orden no tiene un pago pendiente de revision.");
        }

        order.setPaymentReviewNotes(cleanOptional(request.reviewNotes()));
        order.setPaymentReviewedAt(LocalDateTime.now());
        order.setPaymentReviewedBy(normalizeEmail(reviewerEmail));

        if (!request.approved()) {
            order.setStatus(StorefrontOrderStatus.PENDING_PAYMENT);
            return storefrontOrderService.toResponse(storefrontOrderRepository.save(order));
        }

        UUID accountId = request.paymentAccountId() != null
                ? request.paymentAccountId()
                : order.getPaymentAccountId();
        if (accountId == null || order.getPaymentClaimReference() == null || order.getPaymentClaimAmount() == null) {
            throw new BadRequestException("El pago no contiene datos suficientes para aprobarse.");
        }
        PaymentAccount account = paymentAccountService.resolveStorefrontAccount(accountId, order.getCountry());
        if (order.getPaymentClaimAmount().compareTo(order.getTotal()) < 0) {
            throw new BadRequestException("El monto reportado no cubre el total de la orden.");
        }

        StorefrontOrder fulfilled = storefrontFulfillmentService.fulfillPaid(
                order,
                providerName(account),
                order.getPaymentClaimReference(),
                order.getTotal(),
                PaymentMethod.TRANSFER,
                account.getId(),
                request.shouldNotifyClient()
        );
        return storefrontOrderService.toResponse(fulfilled);
    }

    @Transactional
    public StorefrontOrderResponse confirmPayment(String orderNumber, StorefrontPaymentConfirmationRequest request) {
        if (request == null) {
            throw new BadRequestException("Datos de pago requeridos.");
        }
        StorefrontOrder order = findLockedOrder(orderNumber);

        if (order.getStatus() == StorefrontOrderStatus.CANCELLED ||
                order.getStatus() == StorefrontOrderStatus.FAILED ||
                order.getStatus() == StorefrontOrderStatus.REFUNDED) {
            throw new BadRequestException("La orden no puede confirmarse en su estado actual.");
        }
        if (order.getSaleId() != null) {
            return storefrontOrderService.toResponse(order);
        }

        String provider = cleanRequired(request.provider(), "Proveedor de pago requerido.");
        String reference = cleanRequired(request.reference(), "Referencia de pago requerida.");
        storefrontOrderRepository.findByPaymentProviderAndPaymentReference(provider, reference)
                .filter(existing -> !existing.getId().equals(order.getId()))
                .ifPresent(existing -> {
                    throw new BadRequestException("La referencia de pago ya fue utilizada por otra orden.");
                });

        BigDecimal amount = request.amount() != null ? request.amount() : order.getTotal();
        if (amount.compareTo(order.getTotal()) < 0) {
            throw new BadRequestException("El monto confirmado no cubre el total de la orden.");
        }

        StorefrontOrder fulfilled = storefrontFulfillmentService.fulfillPaid(
                order,
                provider,
                reference,
                order.getTotal(),
                request.method() != null ? request.method() : PaymentMethod.CARD,
                request.paymentAccountId(),
                request.shouldNotifyClient()
        );
        return storefrontOrderService.toResponse(fulfilled);
    }

    private StorefrontOrder findLockedOrder(String orderNumber) {
        return storefrontOrderRepository.findByOrderNumberForUpdate(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada: " + orderNumber));
    }

    private void validateOwner(StorefrontOrder order, String authenticatedEmail) {
        String principal = normalizeEmail(authenticatedEmail);
        if (principal == null || !principal.equals(order.getCustomer().getEmail().toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("La orden no pertenece a la cuenta autenticada.");
        }
    }

    private String providerName(PaymentAccount account) {
        String value = account.getBankName() != null ? account.getBankName() : account.getName();
        value = cleanRequired(value, "Banco receptor requerido.");
        return value.length() <= 40 ? value : value.substring(0, 40);
    }

    private String cleanRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value.trim();
    }

    private String cleanOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeEmail(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
