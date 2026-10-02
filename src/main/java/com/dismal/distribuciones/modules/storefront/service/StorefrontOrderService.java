package com.dismal.distribuciones.modules.storefront.service;

import com.dismal.distribuciones.exception.BadRequestException;
import com.dismal.distribuciones.exception.ResourceNotFoundException;
import com.dismal.distribuciones.modules.pricing.service.PricingService;
import com.dismal.distribuciones.modules.security.domain.CustomerType;
import com.dismal.distribuciones.modules.security.domain.Role;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.customer.domain.CustomerMarket;
import com.dismal.distribuciones.modules.security.customer.service.CustomerMarketService;
import com.dismal.distribuciones.modules.security.repository.UserRepository;
import com.dismal.distribuciones.modules.security.util.PhoneNormalizer;
import com.dismal.distribuciones.modules.security.account.service.AccountIdentityService;
import com.dismal.distribuciones.modules.store.domain.Software;
import com.dismal.distribuciones.modules.store.repository.SoftwareRepository;
import com.dismal.distribuciones.modules.store.service.PhysicalInventoryService;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCheckoutMethod;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontOrder;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontOrderItem;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontOrderStatus;
import com.dismal.distribuciones.modules.storefront.dto.StorefrontCustomerRequest;
import com.dismal.distribuciones.modules.storefront.dto.StorefrontOrderItemRequest;
import com.dismal.distribuciones.modules.storefront.dto.StorefrontOrderItemResponse;
import com.dismal.distribuciones.modules.storefront.dto.StorefrontOrderRequest;
import com.dismal.distribuciones.modules.storefront.dto.StorefrontOrderResponse;
import com.dismal.distribuciones.modules.storefront.repository.StorefrontOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorefrontOrderService {

    private static final int MAX_LINE_QUANTITY = 50;
    private static final int MAX_TOTAL_QUANTITY = 200;

    private final StorefrontOrderRepository storefrontOrderRepository;
    private final SoftwareRepository softwareRepository;
    private final UserRepository userRepository;
    private final PricingService pricingService;
    private final PasswordEncoder passwordEncoder;
    private final AccountIdentityService accountIdentityService;
    private final CustomerMarketService customerMarketService;
    private final StorefrontFulfillmentService storefrontFulfillmentService;
    private final PhysicalInventoryService physicalInventoryService;

    @Transactional
    public StorefrontOrderResponse createPendingOrder(StorefrontOrderRequest request) {
        return createPendingOrder(request, null);
    }

    @Transactional
    public StorefrontOrderResponse createPendingOrder(StorefrontOrderRequest request, String authenticatedEmail) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new BadRequestException("La orden debe incluir productos.");
        }

        StorefrontCountry country = StorefrontCountry.from(request.country());
        CustomerType requestedCustomerType = parseCustomerType(request.customerType());
        User customer = resolveCustomer(request.customer(), country, authenticatedEmail);
        CustomerMarket customerMarket = customerMarketService.requireActive(
                customerMarketService.getOrCreate(
                        customer,
                        country,
                        request.customer().taxId(),
                        request.customer().email()
                )
        );
        CustomerType effectiveCustomerType = resolveEffectiveCustomerType(
                customerMarket,
                requestedCustomerType,
                authenticatedEmail
        );
        StorefrontCheckoutMethod checkoutMethod = request.checkoutMethod() != null
                ? request.checkoutMethod()
                : StorefrontCheckoutMethod.BANK_TRANSFER;
        String orderPhone = normalizePhone(request.customer().phone(), country);

        StorefrontOrder order = StorefrontOrder.builder()
                .orderNumber(generateOrderNumber(country))
                .country(country)
                .currency(country.currency())
                .customerType(effectiveCustomerType)
                .status(StorefrontOrderStatus.PENDING_PAYMENT)
                .checkoutMethod(checkoutMethod)
                .customer(customer)
                .customerMarket(customerMarket)
                .email(normalizeEmail(request.customer().email()))
                .firstName(clean(request.customer().firstName()))
                .lastName(clean(request.customer().lastName()))
                .phone(orderPhone)
                .taxId(clean(request.customer().taxId()))
                .shippingRecipient(clean(request.shipping().recipient()))
                .shippingAddressLine1(clean(request.shipping().addressLine1()))
                .shippingAddressLine2(clean(request.shipping().addressLine2()))
                .shippingCity(clean(request.shipping().city()))
                .shippingRegion(clean(request.shipping().region()))
                .shippingPostalCode(clean(request.shipping().postalCode()))
                .shippingCountry(country.name())
                .shippingNotes(clean(request.shipping().notes()))
                .subtotal(BigDecimal.ZERO)
                .discountTotal(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        int totalQuantity = 0;
        for (StorefrontOrderItemRequest itemRequest : mergeItems(request.items()).values()) {
            int quantity = validateQuantity(itemRequest.quantity());
            totalQuantity += quantity;
            if (totalQuantity > MAX_TOTAL_QUANTITY) {
                throw new BadRequestException("La orden supera la cantidad maxima permitida.");
            }

            Software software = physicalInventoryService.reserve(itemRequest.productId(), quantity);
            BigDecimal unitPrice = pricingService.resolvePrice(software, country.name(), effectiveCustomerType);
            BigDecimal lineSubtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            subtotal = subtotal.add(lineSubtotal);

            StorefrontOrderItem item = StorefrontOrderItem.builder()
                    .order(order)
                    .software(software)
                    .productName(software.getName())
                    .platform(software.getPlatform())
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .subtotal(lineSubtotal)
                    .build();
            order.getItems().add(item);
        }

        order.setSubtotal(subtotal);
        order.setTotal(subtotal.subtract(order.getDiscountTotal()));
        if (checkoutMethod == StorefrontCheckoutMethod.CUSTOMER_CREDIT) {
            validateCreditCheckout(customer, customerMarket, authenticatedEmail, order.getTotal());
        }
        StorefrontOrder saved = storefrontOrderRepository.save(order);

        if (checkoutMethod == StorefrontCheckoutMethod.CUSTOMER_CREDIT) {
            saved = storefrontFulfillmentService.fulfillCredit(saved);
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<StorefrontOrderResponse> listOrders(
            StorefrontOrderStatus status,
            int page,
            int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(safePage, safeSize);
        Page<StorefrontOrder> orders = status == null
                ? storefrontOrderRepository.findAllByOrderByCreatedAtDesc(pageable)
                : storefrontOrderRepository.findByStatusOrderByCreatedAtAsc(status, pageable);
        return orders.map(this::toResponse);
    }

    @Transactional
    public StorefrontOrderResponse markReadyForDispatch(String orderNumber) {
        StorefrontOrder order = requireOrderForUpdate(orderNumber);
        if (order.getStatus() != StorefrontOrderStatus.PREPARING) {
            throw new BadRequestException("Solo un pedido en preparacion puede quedar listo para despacho.");
        }
        order.setStatus(StorefrontOrderStatus.READY_FOR_DISPATCH);
        return toResponse(storefrontOrderRepository.save(order));
    }

    @Transactional
    public StorefrontOrderResponse markShipped(String orderNumber, String carrier, String trackingNumber) {
        StorefrontOrder order = requireOrderForUpdate(orderNumber);
        if (order.getStatus() != StorefrontOrderStatus.READY_FOR_DISPATCH) {
            throw new BadRequestException("El pedido debe estar listo antes de despacharlo.");
        }
        order.setShippingCarrier(clean(carrier));
        order.setTrackingNumber(clean(trackingNumber));
        order.setShippedAt(LocalDateTime.now());
        order.setStatus(StorefrontOrderStatus.SHIPPED);
        physicalInventoryService.dispatch(order);
        return toResponse(storefrontOrderRepository.save(order));
    }

    @Transactional
    public StorefrontOrderResponse markDelivered(String orderNumber) {
        StorefrontOrder order = requireOrderForUpdate(orderNumber);
        if (order.getStatus() != StorefrontOrderStatus.SHIPPED) {
            throw new BadRequestException("Solo un pedido despachado puede marcarse como entregado.");
        }
        order.setDeliveredAt(LocalDateTime.now());
        order.setStatus(StorefrontOrderStatus.DELIVERED);
        return toResponse(storefrontOrderRepository.save(order));
    }

    private StorefrontOrder requireOrderForUpdate(String orderNumber) {
        return storefrontOrderRepository.findByOrderNumberForUpdate(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado."));
    }

    private void validateCreditCheckout(
            User customer,
            CustomerMarket customerMarket,
            String authenticatedEmail,
            BigDecimal orderTotal
    ) {
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            throw new BadRequestException("Inicia sesion para comprar con credito Dismal.");
        }
        if (!customer.isEmailVerified()) {
            throw new BadRequestException("Confirma tu correo antes de utilizar el credito aprobado.");
        }
        if (!customerMarket.isHasCredit()) {
            throw new BadRequestException("La cuenta no tiene credito habilitado.");
        }
        BigDecimal creditLimit = customerMarket.getCreditLimit() != null
                ? customerMarket.getCreditLimit() : BigDecimal.ZERO;
        BigDecimal creditUsed = customerMarket.getCreditUsed() != null
                ? customerMarket.getCreditUsed() : BigDecimal.ZERO;
        BigDecimal available = creditLimit.subtract(creditUsed);
        if (available.signum() <= 0) {
            throw new BadRequestException("La cuenta no tiene cupo de credito disponible.");
        }
        if (orderTotal != null && available.compareTo(orderTotal) < 0) {
            throw new BadRequestException("El cupo disponible no cubre el total de la orden.");
        }
    }

    private Map<UUID, StorefrontOrderItemRequest> mergeItems(List<StorefrontOrderItemRequest> items) {
        Map<UUID, StorefrontOrderItemRequest> merged = new HashMap<>();
        for (StorefrontOrderItemRequest item : items) {
            if (item == null || item.productId() == null) {
                throw new BadRequestException("Producto requerido.");
            }
            int quantity = validateQuantity(item.quantity());
            StorefrontOrderItemRequest existing = merged.get(item.productId());
            if (existing == null) {
                merged.put(item.productId(), new StorefrontOrderItemRequest(item.productId(), quantity));
            } else {
                merged.put(item.productId(), new StorefrontOrderItemRequest(item.productId(), existing.quantity() + quantity));
            }
        }
        return merged;
    }

    private int validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BadRequestException("La cantidad debe ser mayor que cero.");
        }
        if (quantity > MAX_LINE_QUANTITY) {
            throw new BadRequestException("La cantidad por producto supera el maximo permitido.");
        }
        return quantity;
    }

    private User resolveCustomer(
            StorefrontCustomerRequest request,
            StorefrontCountry country,
            String authenticatedEmail
    ) {
        if (request == null) {
            throw new BadRequestException("Datos de cliente requeridos.");
        }
        String email = normalizeEmail(request.email());
        String phone = normalizePhone(request.phone(), country);
        String principalEmail = authenticatedEmail != null ? normalizeEmail(authenticatedEmail) : null;
        User existing = userRepository.findByEmail(email).orElse(null);

        if (principalEmail != null) {
            if (!principalEmail.equals(email)) {
                throw new BadRequestException("El email de la orden debe coincidir con la cuenta autenticada.");
            }
            if (existing == null) {
                throw new ResourceNotFoundException("La cuenta autenticada no existe.");
            }
            if (!existing.isEnabled()) {
                throw new BadRequestException("La cuenta del cliente esta deshabilitada.");
            }
            existing.setFirstname(clean(request.firstName()));
            existing.setLastname(clean(request.lastName()));
            if (phone != null) {
                existing.setPhone(phone);
            }
            return userRepository.save(existing);
        }

        if (existing != null) {
            throw new BadRequestException("Este correo ya tiene una cuenta. Inicia sesion para continuar.");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new BadRequestException("Crea una contrasena para guardar tus compras y licencias.");
        }

        User created = User.builder()
                .firstname(clean(request.firstName()))
                .lastname(clean(request.lastName()))
                .email(email)
                .phone(phone)
                .taxId(request.taxId() != null && !request.taxId().isBlank() ? request.taxId().trim() : null)
                .billingEmail(email)
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .customerType(CustomerType.FINAL)
                .enabled(true)
                .build();
        User saved = userRepository.save(created);
        accountIdentityService.issueVerification(saved, country.name());
        return saved;
    }

    private CustomerType resolveEffectiveCustomerType(
            CustomerMarket customerMarket,
            CustomerType requestedCustomerType,
            String authenticatedEmail
    ) {
        if (authenticatedEmail != null && requestedCustomerType == CustomerType.DISTRIBUTOR
                && customerMarket.getCustomerType() == CustomerType.DISTRIBUTOR) {
            return CustomerType.DISTRIBUTOR;
        }
        return CustomerType.FINAL;
    }

    private CustomerType parseCustomerType(String value) {
        if (value == null || value.isBlank()) {
            return CustomerType.FINAL;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals("WHOLESALE") || normalized.equals("MAYORISTA")) {
            return CustomerType.DISTRIBUTOR;
        }
        if (normalized.equals("DISTRIBUTOR") || normalized.equals("DISTRIBUIDOR")) {
            return CustomerType.DISTRIBUTOR;
        }
        if (normalized.equals("FINAL") || normalized.equals("CUSTOMER")) {
            return CustomerType.FINAL;
        }
        throw new BadRequestException("Tipo de cliente no soportado: " + value);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email requerido.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone, StorefrontCountry country) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String trimmed = phone.trim();
        String digits = trimmed.replaceAll("[^0-9+]", "");
        if (!digits.startsWith("+")) {
            String onlyDigits = digits.replaceAll("[^0-9]", "");
            if (country == StorefrontCountry.PE && onlyDigits.length() == 9) {
                trimmed = "+51" + onlyDigits;
            } else if (country == StorefrontCountry.EC && onlyDigits.length() == 10 && onlyDigits.startsWith("0")) {
                trimmed = "+593" + onlyDigits.substring(1);
            }
        }
        return PhoneNormalizer.normalize(trimmed);
    }

    private String clean(String value) {
        return value != null ? value.trim() : null;
    }

    private String generateOrderNumber(StorefrontCountry country) {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int attempt = 0; attempt < 10; attempt += 1) {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
            String orderNumber = "MS-" + country.name() + "-" + date + "-" + suffix;
            if (!storefrontOrderRepository.existsByOrderNumber(orderNumber)) {
                return orderNumber;
            }
        }
        throw new BadRequestException("No se pudo generar numero de orden.");
    }

    public StorefrontOrderResponse toResponse(StorefrontOrder order) {
        return new StorefrontOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getCountry(),
                order.getCurrency(),
                order.getCustomerType(),
                order.getCheckoutMethod(),
                order.getStatus() == StorefrontOrderStatus.PENDING_PAYMENT ||
                order.getStatus() == StorefrontOrderStatus.PAYMENT_REVIEW,
                order.getCustomer().getId(),
                order.getEmail(),
                order.getFirstName(),
                order.getLastName(),
                order.getPhone(),
                order.getTaxId(),
                order.getShippingRecipient(),
                order.getShippingAddressLine1(),
                order.getShippingAddressLine2(),
                order.getShippingCity(),
                order.getShippingRegion(),
                order.getShippingPostalCode(),
                order.getShippingCountry(),
                order.getShippingNotes(),
                order.getShippingCarrier(),
                order.getTrackingNumber(),
                order.getShippedAt(),
                order.getDeliveredAt(),
                order.getSubtotal(),
                order.getDiscountTotal(),
                order.getTotal(),
                order.getPaymentAccountId(),
                order.getPaymentClaimBank(),
                order.getPaymentClaimReference(),
                order.getPaymentClaimPayer(),
                order.getPaymentClaimAmount(),
                order.getPaymentClaimedAt(),
                order.getPaymentReviewNotes(),
                order.getCreatedAt(),
                order.getItems().stream()
                        .map(item -> new StorefrontOrderItemResponse(
                                item.getSoftware().getId(),
                                item.getProductName(),
                                item.getPlatform(),
                                item.getQuantity(),
                                item.getUnitPrice(),
                                item.getSubtotal()
                        ))
                        .toList()
        );
    }
}
