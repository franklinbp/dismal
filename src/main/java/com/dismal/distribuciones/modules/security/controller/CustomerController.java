package com.dismal.distribuciones.modules.security.controller;

import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.dto.CustomerDTO;
import com.dismal.distribuciones.modules.security.dto.CustomerSummaryDTO;
import com.dismal.distribuciones.modules.security.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    // Using a Record as a DTO for the update request to ensure only specific fields are processed.
    public record CustomerUpdateRequest(
            String firstname,
            String lastname,
            String phone,
            String taxId,
            String billingEmail,
            boolean hasCredit,
            BigDecimal creditLimit,
            Integer creditDays
    ) {}

    @GetMapping
    public ResponseEntity<List<CustomerDTO>> listCustomers() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(customerService.listCustomers());
    }

    @GetMapping("/summary")
    public ResponseEntity<List<CustomerSummaryDTO>> getCustomerSummaries() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(customerService.getCustomerSummaries());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerDTO> updateCustomer(@PathVariable UUID id, @RequestBody CustomerUpdateRequest request) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        // Map DTO to a temporary User object for the service layer
        User userDetails = User.builder()
                .firstname(request.firstname())
                .lastname(request.lastname())
                .phone(request.phone())
                .taxId(request.taxId())
                .billingEmail(request.billingEmail())
                .hasCredit(request.hasCredit())
                .creditLimit(request.creditLimit())
                .creditDays(request.creditDays())
                .build();
        CustomerDTO updatedUser = customerService.updateCustomerDetails(id, userDetails);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable UUID id) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        customerService.softDeleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    private Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication;
    }

    private boolean isAdminOrManager(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ADMIN")
                        || authority.getAuthority().equals("MANAGER"));
    }
}
