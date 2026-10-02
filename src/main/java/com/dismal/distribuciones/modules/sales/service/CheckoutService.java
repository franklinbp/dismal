package com.dismal.distribuciones.modules.sales.service;

import com.dismal.distribuciones.exception.InsufficientCreditException;
import com.dismal.distribuciones.exception.InsufficientStockException;
import com.dismal.distribuciones.exception.ResourceNotFoundException;
import com.dismal.distribuciones.modules.sales.domain.Order;
import com.dismal.distribuciones.modules.sales.event.OrderPaidEvent;
import com.dismal.distribuciones.modules.sales.repository.OrderRepository;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.repository.UserRepository;
import com.dismal.distribuciones.modules.store.domain.License;
import com.dismal.distribuciones.modules.store.domain.LicenseStatus;
import com.dismal.distribuciones.modules.store.repository.LicenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final LicenseRepository licenseRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Order purchaseSoftware(UUID softwareId, UUID userId) {
        // 1. Find the user making the purchase
        User customer = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // 2. Find an available license for the software
        List<License> availableLicenses = licenseRepository.findAvailableLicenses(softwareId, PageRequest.of(0, 1));
        if (availableLicenses.isEmpty()) {
            throw new InsufficientStockException("No stock available for software ID: " + softwareId);
        }
        License licenseToSell = availableLicenses.get(0);
        if (licenseToSell.getStatus() != LicenseStatus.ACTIVE || !licenseToSell.isAvailable()) {
            throw new InsufficientStockException("No stock available for software ID: " + softwareId);
        }
        BigDecimal salePrice = licenseToSell.getSoftware().getPrice();

        // 3. Credit Check
        if (customer.isHasCredit()) {
            if (customer.getCreditLimit().compareTo(salePrice) < 0) {
                throw new InsufficientCreditException(
                        String.format("Purchase of %.2f exceeds credit limit of %.2f for user %s",
                                salePrice, customer.getCreditLimit(), customer.getEmail())
                );
            }
        }
        // If user does not have credit, we assume a different payment method is used (e.g., credit card).
        // For this example, we proceed. A real implementation would have more complex payment logic.

        // 4. Mark license as sold (consume one activation)
        licenseToSell.setOwner(customer);
        licenseToSell.setUsedActivations(licenseToSell.getUsedActivations() + 1);
        License soldLicense = licenseRepository.save(licenseToSell);

        // 5. Create a historical order record
        Order order = Order.builder()
                .customer(customer)
                .purchasedSoftware(soldLicense.getSoftware())
                .assignedLicense(soldLicense)
                .salePrice(salePrice) // Record price at time of sale
                .build();
        orderRepository.save(order);

        // 6. Publish an event for other parts of the system to react to (e.g., n8n webhook)
        OrderPaidEvent event = new OrderPaidEvent(
                this,
                customer.getEmail(),
                customer.getPhone(),
                soldLicense.getSoftware().getName(),
                soldLicense.getLicenseKey() // The converter ensures this is plaintext
        );
        eventPublisher.publishEvent(event);

        return order;
    }
}
