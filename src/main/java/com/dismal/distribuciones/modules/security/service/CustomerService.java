package com.dismal.distribuciones.modules.security.service;

import com.dismal.distribuciones.exception.ResourceNotFoundException;
import com.dismal.distribuciones.modules.integrations.crm.event.CustomerChangedCrmSyncEvent;
import com.dismal.distribuciones.modules.sales.repository.CustomerBalanceView;
import com.dismal.distribuciones.modules.sales.repository.InvoiceRepository;
import com.dismal.distribuciones.modules.security.domain.Role;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.domain.CustomerType;
import com.dismal.distribuciones.modules.security.dto.CustomerDTO;
import com.dismal.distribuciones.modules.security.dto.CustomerSummaryDTO;
import com.dismal.distribuciones.modules.security.repository.UserRepository;
import com.dismal.distribuciones.modules.security.util.PhoneNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<CustomerDTO> listCustomers() {
        return listCustomerEntities().stream()
                .map(this::toCustomerDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CustomerSummaryDTO> getCustomerSummaries() {
        // 1. Get all pending balances in a single, efficient query, mapped by user ID
        Map<UUID, BigDecimal> pendingBalances = invoiceRepository.findPendingBalances().stream()
                .collect(Collectors.toMap(CustomerBalanceView::getUserId, CustomerBalanceView::getTotalPending));

        // 2. Get all customers
        List<User> customers = listCustomerEntities();

        // 3. Map customers to DTOs, looking up the pending balance from the map
        return customers.stream()
                .map(user -> new CustomerSummaryDTO(
                        user.getId(),
                        user.getFirstname() + " " + user.getLastname(),
                        user.getTaxId(),
                        user.getPhone(),
                        pendingBalances.getOrDefault(user.getId(), BigDecimal.ZERO)
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public CustomerDTO updateCustomerDetails(UUID userId, User details) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Update customer-specific fields
        user.setTaxId(details.getTaxId());
        user.setBillingEmail(details.getBillingEmail());
        if (details.getCustomerType() != null) {
            user.setCustomerType(details.getCustomerType());
        }
        user.setHasCredit(details.isHasCredit());
        user.setCreditLimit(details.getCreditLimit());
        user.setCreditDays(details.getCreditDays());
        user.setPhone(PhoneNormalizer.normalize(details.getPhone()));
        // Basic user info can also be updated here if needed
        user.setFirstname(details.getFirstname());
        user.setLastname(details.getLastname());

        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new CustomerChangedCrmSyncEvent(this, saved.getId()));
        return toCustomerDTO(saved);
    }

    @Transactional
    public void softDeleteCustomer(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        user.setEnabled(false); // Logical delete
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new CustomerChangedCrmSyncEvent(this, saved.getId()));
    }

    private List<User> listCustomerEntities() {
        return userRepository.findByRoleInAndEnabledTrue(List.of(Role.USER, Role.CUSTOMER));
    }

    private CustomerDTO toCustomerDTO(User user) {
        return new CustomerDTO(
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
                user.getCreditDays(),
                user.isEnabled()
        );
    }
}
