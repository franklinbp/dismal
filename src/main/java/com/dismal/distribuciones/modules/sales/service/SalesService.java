package com.dismal.distribuciones.modules.sales.service;

import com.dismal.distribuciones.exception.BadRequestException;
import com.dismal.distribuciones.exception.InsufficientCreditException;
import com.dismal.distribuciones.exception.InsufficientStockException;
import com.dismal.distribuciones.exception.ResourceNotFoundException;
import com.dismal.distribuciones.modules.sales.domain.*;
import com.dismal.distribuciones.modules.sales.dto.CreateSaleRequest;
import com.dismal.distribuciones.modules.sales.dto.SaleItemResponse;
import com.dismal.distribuciones.modules.sales.dto.SaleResponse;
import com.dismal.distribuciones.modules.sales.repository.AccountsReceivableRepository;
import com.dismal.distribuciones.modules.sales.repository.PaymentRepository;
import com.dismal.distribuciones.modules.sales.repository.SaleFulfillmentRepository;
import com.dismal.distribuciones.modules.sales.repository.SaleRepository;
import com.dismal.distribuciones.modules.integrations.notifications.domain.NotificationChannel;
import com.dismal.distribuciones.modules.integrations.outbox.service.OutboxService;
import com.dismal.distribuciones.modules.integrations.outbox.repository.EventOutboxRepository;
import com.dismal.distribuciones.modules.integrations.notifications.repository.NotificationDeliveryLogRepository;
import com.dismal.distribuciones.modules.integrations.notifications.service.EmailNotificationService;
import com.dismal.distribuciones.modules.integrations.notifications.service.NotificationDeliveryLogService;
import com.dismal.distribuciones.modules.integrations.crm.service.SaleWhatsappPublisherService;
import com.dismal.distribuciones.modules.planning.repository.SalesTargetRepository;
import com.dismal.distribuciones.modules.pricing.service.PricingService;
import com.dismal.distribuciones.modules.security.domain.Role;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.customer.domain.CustomerMarket;
import com.dismal.distribuciones.modules.security.customer.service.CustomerMarketService;
import com.dismal.distribuciones.modules.security.repository.UserRepository;
import com.dismal.distribuciones.modules.store.domain.License;
import com.dismal.distribuciones.modules.store.domain.Software;
import com.dismal.distribuciones.modules.store.repository.LicenseRepository;
import com.dismal.distribuciones.modules.store.repository.SoftwareRepository;
import com.dismal.distribuciones.modules.sales.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SalesService {

    private final SaleRepository saleRepository;
    private final SoftwareRepository softwareRepository;
    private final UserRepository userRepository;
    private final LicenseRepository licenseRepository;
    private final AccountsReceivableRepository accountsReceivableRepository;
    private final SaleFulfillmentRepository saleFulfillmentRepository;
    private final PaymentRepository paymentRepository;
    private final OutboxService outboxService;
    private final InvoiceRepository invoiceRepository;
    private final EventOutboxRepository eventOutboxRepository;
    private final NotificationDeliveryLogRepository notificationDeliveryLogRepository;
    private final EmailNotificationService emailNotificationService;
    private final NotificationDeliveryLogService notificationDeliveryLogService;
    private final SaleWhatsappPublisherService saleWhatsappPublisherService;
    private final SalesTargetRepository salesTargetRepository;
    private final PricingService pricingService;
    private final CustomerMarketService customerMarketService;
    private final SaleNumberService saleNumberService;

    @Transactional
    public SaleResponse createSale(CreateSaleRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new BadRequestException("Sale items are required");
        }
        validateRequestedLicenseSelections(request.items());

        User client = userRepository.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + request.clientId()));
        if (!(client.getRole() == Role.USER || client.getRole() == Role.CUSTOMER)) {
            throw new BadRequestException("Client role must be USER or CUSTOMER");
        }
        CustomerMarket customerMarket = customerMarketService.resolveForSale(client, request.country());

        Sale sale = Sale.builder()
                .client(client)
                .customerMarket(customerMarket)
                .country(customerMarket.getCountry())
                .currency(customerMarket.getCurrency())
                .saleType(request.saleType())
                .status(SaleStatus.DRAFT)
                .total(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (var item : request.items()) {
            total = total.add(addSaleItem(sale, client, customerMarket, item));
        }

        sale.setTotal(total);
        Sale saved = saleRepository.save(sale);
        return toSaleResponse(saved);
    }

    @Transactional
    public SaleResponse updateSale(UUID saleId, CreateSaleRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new BadRequestException("Sale items are required");
        }
        validateRequestedLicenseSelections(request.items());

        Sale lockedSale = saleRepository.findByIdForUpdate(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with ID: " + saleId));
        Sale sale = saleRepository.findWithItemsById(saleId).orElse(lockedSale);
        if (sale.getStatus() != SaleStatus.DRAFT) {
            throw new BadRequestException("Only draft sales can be updated");
        }

        User client = userRepository.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + request.clientId()));
        if (!(client.getRole() == Role.USER || client.getRole() == Role.CUSTOMER)) {
            throw new BadRequestException("Client role must be USER or CUSTOMER");
        }
        CustomerMarket customerMarket = customerMarketService.resolveForSale(client, request.country());

        sale.setClient(client);
        sale.setCustomerMarket(customerMarket);
        sale.setCountry(customerMarket.getCountry());
        sale.setCurrency(customerMarket.getCurrency());
        sale.setSaleType(request.saleType());
        sale.getItems().clear();

        BigDecimal total = BigDecimal.ZERO;
        for (var item : request.items()) {
            total = total.add(addSaleItem(sale, client, customerMarket, item));
        }

        sale.setTotal(total);
        Sale saved = saleRepository.save(sale);
        return toSaleResponse(saved);
    }

    @Transactional
    public SaleResponse confirmSale(UUID saleId) {
        return confirmSale(saleId, null);
    }

    @Transactional
    public SaleResponse confirmSale(UUID saleId, List<NotificationChannel> deliveryChannels) {
        Sale lockedSale = saleRepository.findByIdForUpdate(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with ID: " + saleId));
        Sale sale = saleRepository.findWithItemsById(saleId).orElse(lockedSale);

        if (sale.getStatus() == SaleStatus.CANCELLED) {
            throw new IllegalStateException("Cannot confirm a cancelled sale");
        }
        if (sale.getStatus() != SaleStatus.DRAFT) {
            return toSaleResponse(sale);
        }

        AccountsReceivable ar = null;
        List<License> assigned;
        UUID deliveryOutboxId;
        validateInventoryForSale(sale);
        if (sale.getSaleType() == SaleType.CREDIT) {
            validateCredit(sale);
            assignSaleNumber(sale);
            sale.setStatus(SaleStatus.CONFIRMED);
            assigned = assignLicensesForSale(sale);
            ar = createAccountsReceivable(sale);
            deliveryOutboxId = outboxService.enqueueLicenseDeliveredEvent(
                    sale,
                    assigned,
                    ar.getDueDate(),
                    ar.getBalance(),
                    deliveryChannels
            );
        } else {
            assignSaleNumber(sale);
            sale.setStatus(SaleStatus.CONFIRMED);
            assigned = assignLicensesForSale(sale);
            ar = createAccountsReceivable(sale);
            deliveryOutboxId = outboxService.enqueueLicenseDeliveredEvent(
                    sale,
                    assigned,
                    ar.getDueDate(),
                    ar.getBalance(),
                    deliveryChannels
            );
        }

        Sale saved = saleRepository.save(sale);
        incrementTargetsForSale(saved, 1);
        if (allowsChannel(deliveryChannels, NotificationChannel.EMAIL)) {
            recordEmailDelivery(deliveryOutboxId, emailNotificationService.sendLicensesDelivered(saved, ar, assigned));
        }
        saleWhatsappPublisherService.publishSaleConfirmed(saved, ar, assigned, deliveryChannels, deliveryOutboxId);
        return toSaleResponse(saved);
    }

    private void recordEmailDelivery(UUID outboxEventId, com.dismal.distribuciones.modules.integrations.notifications.dto.NotificationSendResult result) {
        if (outboxEventId == null || result == null) {
            return;
        }
        if (result.sent()) {
            notificationDeliveryLogService.markSent(outboxEventId, NotificationChannel.EMAIL, "SMTP");
        } else {
            notificationDeliveryLogService.markFailed(outboxEventId, NotificationChannel.EMAIL, result.message());
        }
    }

    private boolean allowsChannel(List<NotificationChannel> channels, NotificationChannel channel) {
        return channels == null || channels.isEmpty() || channels.contains(channel);
    }

    @Transactional
    public SaleResponse cancelSale(UUID saleId) {
        Sale sale = saleRepository.findWithItemsById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with ID: " + saleId));
        if (sale.getStatus() == SaleStatus.CANCELLED) {
            return toSaleResponse(sale);
        }
        boolean wasConfirmed = sale.getStatus() == SaleStatus.CONFIRMED;
        if (sale.getStatus() == SaleStatus.PAID) {
            throw new BadRequestException("Cannot cancel a paid sale");
        }
        sale.setStatus(SaleStatus.CANCELLED);
        AccountsReceivable ar = accountsReceivableRepository.findBySaleId(sale.getId()).orElse(null);
        outboxService.enqueueSaleCancelledEvent(
                sale,
                ar != null ? ar.getDueDate() : null,
                ar != null ? ar.getBalance() : BigDecimal.ZERO
        );
        releaseFulfillments(sale);
        revertFinancialsOnCancel(sale, ar);
        Sale saved = saleRepository.save(sale);
        if (wasConfirmed) {
            incrementTargetsForSale(saved, -1);
        }
        return toSaleResponse(saved);
    }

    @Transactional
    public void cleanupSaleFinancials(UUID saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with ID: " + saleId));

        AccountsReceivable ar = accountsReceivableRepository.findBySaleId(sale.getId()).orElse(null);
        if (ar != null) {
            if (sale.getSaleType() == SaleType.CREDIT && ar.getStatus() != AccountsReceivableStatus.PAID) {
                releaseCredit(sale);
            }
            accountsReceivableRepository.deleteBySaleId(sale.getId());
        }

        paymentRepository.deleteBySaleId(sale.getId());
        invoiceRepository.deleteBySaleId(sale.getId());

        List<com.dismal.distribuciones.modules.integrations.outbox.domain.EventOutbox> outboxEvents =
                eventOutboxRepository.findByAggregateId(sale.getId());
        if (!outboxEvents.isEmpty()) {
            List<UUID> outboxIds = outboxEvents.stream()
                    .map(com.dismal.distribuciones.modules.integrations.outbox.domain.EventOutbox::getId)
                    .toList();
            notificationDeliveryLogRepository.deleteByOutboxEventIdIn(outboxIds);
            eventOutboxRepository.deleteByAggregateId(sale.getId());
        }
    }

    @Transactional
    public void deleteSale(UUID saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with ID: " + saleId));

        List<SaleFulfillment> fulfillments = saleFulfillmentRepository.findBySaleIdWithLicense(sale.getId());
        if (!fulfillments.isEmpty()) {
            List<License> licenses = new ArrayList<>();
            for (SaleFulfillment fulfillment : fulfillments) {
                License license = fulfillment.getLicense();
                if (license == null) {
                    continue;
                }
                int used = license.getUsedActivations() != null ? license.getUsedActivations() : 0;
                license.setUsedActivations(Math.max(used - 1, 0));
                licenses.add(license);
            }
            if (!licenses.isEmpty()) {
                licenseRepository.saveAll(licenses);
            }
            saleFulfillmentRepository.deleteBySaleId(sale.getId());
        }

        cleanupSaleFinancials(sale.getId());

        saleRepository.delete(sale);
    }

    @Transactional(readOnly = true)
    public SaleResponse getSale(UUID saleId) {
        Sale sale = saleRepository.findWithItemsById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with ID: " + saleId));
        return toSaleResponse(sale);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<SaleResponse> listSales(UUID clientId, SaleStatus status, SaleType saleType,
                                        LocalDate fromDate, LocalDate toDate, int page, int size) {
        return listSales(clientId, status, saleType, null, fromDate, toDate, page, size);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<SaleResponse> listSales(
            UUID clientId,
            SaleStatus status,
            SaleType saleType,
            com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry country,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size
    ) {
        Specification<Sale> spec = Specification.where(null);
        if (clientId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("client").get("id"), clientId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        } else {
            spec = spec.and((root, query, cb) -> cb.notEqual(root.get("status"), SaleStatus.CANCELLED));
        }
        if (saleType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("saleType"), saleType));
        }
        if (country != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("country"), country));
        }
        if (fromDate != null) {
            LocalDateTime fromDateTime = fromDate.atStartOfDay();
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), fromDateTime));
        }
        if (toDate != null) {
            LocalDateTime toDateTime = toDate.plusDays(1).atStartOfDay();
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), toDateTime));
        }

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "createdAt", "id")
        );
        var salesPage = saleRepository.findAll(spec, pageable);
        List<Sale> sales = salesPage.getContent();
        Map<UUID, BigDecimal> paidBySale = getPaidBySaleIds(sales);
        List<SaleResponse> content = sales.stream()
                .map(sale -> toSaleResponse(sale, paidBySale.getOrDefault(sale.getId(), BigDecimal.ZERO)))
                .toList();
        return new org.springframework.data.domain.PageImpl<>(content, pageable, salesPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<com.dismal.distribuciones.modules.sales.dto.SaleLicenseResponse> listSaleLicenses(UUID saleId) {
        List<SaleFulfillment> fulfillments = saleFulfillmentRepository.findBySaleIdWithLicense(saleId);
        return fulfillments.stream()
                .map(fulfillment -> new com.dismal.distribuciones.modules.sales.dto.SaleLicenseResponse(
                        fulfillment.getId(),
                        fulfillment.getLicense().getId(),
                        fulfillment.getLicense().getLicenseKey(),
                        fulfillment.getLicense().getSoftware().getId(),
                        fulfillment.getLicense().getSoftware().getName(),
                        fulfillment.getAssignedAt()
                ))
                .toList();
    }

    private BigDecimal addSaleItem(
            Sale sale,
            User client,
            CustomerMarket customerMarket,
            com.dismal.distribuciones.modules.sales.dto.SaleItemRequest item
    ) {
        if (item.quantity() == null || item.quantity() <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }
        validateSelectedLicenses(item);
        Software software = softwareRepository.findById(item.softwareId())
                .orElseThrow(() -> new ResourceNotFoundException("Software not found with ID: " + item.softwareId()));
        BigDecimal unitPrice = item.unitPrice() != null
                ? item.unitPrice()
                : pricingService.resolvePrice(
                        software,
                        customerMarket.getCountry().name(),
                        customerMarket.getCustomerType()
                );
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.quantity()));

        SaleItem saleItem = SaleItem.builder()
                .sale(sale)
                .software(software)
                .quantity(item.quantity())
                .unitPrice(unitPrice)
                .subtotal(subtotal)
                .build();
        saleItem.setSelectedLicenseIdList(item.selectedLicenseIds());
        sale.getItems().add(saleItem);
        return subtotal;
    }

    private void validateInventoryForSale(Sale sale) {
        List<com.dismal.distribuciones.modules.sales.dto.SaleItemRequest> requestItems = toSaleItemRequests(sale);
        for (int index = 0; index < sale.getItems().size(); index++) {
            SaleItem item = sale.getItems().get(index);
            com.dismal.distribuciones.modules.sales.dto.SaleItemRequest requestItem = requestItems.get(index);
            int required = item.getQuantity() != null ? item.getQuantity() : 0;
            if (required <= 0) {
                continue;
            }
            int available = availableActivations(item.getSoftware().getId(), required, requestItem.selectedLicenseIds());
            if (available < required) {
                throw new InsufficientStockException("Not enough activations for software ID: " +
                        item.getSoftware().getId());
            }
        }
    }

    private void validateCredit(Sale sale) {
        if (sale.getCustomerMarket() != null) {
            CustomerMarket market = customerMarketService.lock(sale.getCustomerMarket());
            sale.setCustomerMarket(market);
            if (!market.isHasCredit()) {
                throw new InsufficientCreditException("Client does not have credit enabled for this market");
            }
            BigDecimal creditLimit = safeBigDecimal(market.getCreditLimit());
            BigDecimal creditUsed = safeBigDecimal(market.getCreditUsed());
            BigDecimal available = creditLimit.subtract(creditUsed);
            if (available.compareTo(sale.getTotal()) < 0) {
                throw new InsufficientCreditException("Insufficient credit for this market");
            }
            market.setCreditUsed(creditUsed.add(sale.getTotal()));
            customerMarketService.saveCredit(market);
            return;
        }
        User client = userRepository.findByIdForUpdate(sale.getClient().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + sale.getClient().getId()
                ));
        sale.setClient(client);
        if (!client.isHasCredit()) {
            throw new InsufficientCreditException("Client does not have credit enabled");
        }
        BigDecimal creditLimit = safeBigDecimal(client.getCreditLimit());
        BigDecimal creditUsed = safeBigDecimal(client.getCreditUsed());
        BigDecimal available = creditLimit.subtract(creditUsed);
        if (available.compareTo(sale.getTotal()) < 0) {
            throw new InsufficientCreditException("Insufficient credit for this sale");
        }
        client.setCreditUsed(creditUsed.add(sale.getTotal()));
        userRepository.save(client);
    }

    private AccountsReceivable createAccountsReceivable(Sale sale) {
        var existing = accountsReceivableRepository.findBySaleId(sale.getId());
        if (existing.isPresent()) {
            return existing.get();
        }
        User client = sale.getClient();
        int creditDays = 0;
        if (sale.getSaleType() == SaleType.CREDIT) {
            if (sale.getCustomerMarket() != null && sale.getCustomerMarket().getCreditDays() != null) {
                creditDays = sale.getCustomerMarket().getCreditDays();
            } else if (client.getCreditDays() != null) {
                creditDays = client.getCreditDays();
            }
        }
        AccountsReceivable ar = AccountsReceivable.builder()
                .sale(sale)
                .client(client)
                .total(sale.getTotal())
                .paid(BigDecimal.ZERO)
                .balance(sale.getTotal())
                .dueDate(LocalDate.now().plusDays(creditDays))
                .status(AccountsReceivableStatus.OPEN)
                .build();
        return accountsReceivableRepository.save(ar);
    }

    private List<License> assignLicensesForSale(Sale sale) {
        List<License> activationAssignments = new ArrayList<>();
        java.util.Map<UUID, License> updates = new java.util.LinkedHashMap<>();
        List<com.dismal.distribuciones.modules.sales.dto.SaleItemRequest> requestItems = toSaleItemRequests(sale);

        for (int index = 0; index < sale.getItems().size(); index++) {
            SaleItem item = sale.getItems().get(index);
            com.dismal.distribuciones.modules.sales.dto.SaleItemRequest requestItem = requestItems.get(index);
            int remaining = item.getQuantity() != null ? item.getQuantity() : 0;
            if (remaining <= 0) {
                continue;
            }

            List<License> selectedLicenses = resolveSelectedLicenses(item.getSoftware().getId(), requestItem.selectedLicenseIds());
            for (License license : selectedLicenses) {
                int consume = consumeLicenseForSale(license, sale.getClient(), remaining, updates, activationAssignments);
                remaining -= consume;
                if (remaining <= 0) {
                    break;
                }
            }

            if (remaining <= 0) {
                continue;
            }

            Set<UUID> excluded = selectedLicenses.stream().map(License::getId).collect(java.util.stream.Collectors.toSet());
            List<License> licenses = fetchAvailableLicenses(item.getSoftware().getId(), remaining, excluded);
            int capacity = totalAvailableActivations(licenses);
            if (capacity < remaining) {
                throw new InsufficientStockException("Not enough activations for software ID: " +
                        item.getSoftware().getId());
            }
            for (License license : licenses) {
                if (remaining <= 0) {
                    break;
                }
                remaining -= consumeLicenseForSale(license, sale.getClient(), remaining, updates, activationAssignments);
            }
        }

        if (!updates.isEmpty()) {
            licenseRepository.saveAll(updates.values());
        }

        List<SaleFulfillment> fulfillments = activationAssignments.stream()
                .map(license -> SaleFulfillment.builder()
                        .sale(sale)
                        .license(license)
                        .activationCost(calculateActivationCost(license))
                        .build())
                .toList();
        if (!fulfillments.isEmpty()) {
            saleFulfillmentRepository.saveAll(fulfillments);
        }
        return activationAssignments;
    }

    private int availableActivations(UUID softwareId, int required, List<UUID> selectedLicenseIds) {
        List<License> selectedLicenses = resolveSelectedLicenses(softwareId, selectedLicenseIds);
        int selectedCapacity = totalAvailableActivations(selectedLicenses);
        if (selectedCapacity >= required) {
            return selectedCapacity;
        }
        Set<UUID> excluded = selectedLicenses.stream().map(License::getId).collect(java.util.stream.Collectors.toSet());
        List<License> licenses = fetchAvailableLicenses(softwareId, required - selectedCapacity, excluded);
        return selectedCapacity + totalAvailableActivations(licenses);
    }

    private int totalAvailableActivations(List<License> licenses) {
        int total = 0;
        for (License license : licenses) {
            int used = license.getUsedActivations() != null ? license.getUsedActivations() : 0;
            int max = license.getMaxActivations() != null ? license.getMaxActivations() : 0;
            int available = Math.max(max - used, 0);
            total += available;
        }
        return total;
    }

    private List<License> fetchAvailableLicenses(UUID softwareId, int required, Set<UUID> excludedLicenseIds) {
        int page = 0;
        int size = 50;
        List<License> collected = new ArrayList<>();
        int capacity = 0;
        while (true) {
            List<License> batch = licenseRepository.findAvailableLicenses(softwareId, PageRequest.of(page, size));
            if (batch.isEmpty()) {
                break;
            }
            for (License license : batch) {
                if (excludedLicenseIds != null && excludedLicenseIds.contains(license.getId())) {
                    continue;
                }
                collected.add(license);
                int used = license.getUsedActivations() != null ? license.getUsedActivations() : 0;
                int max = license.getMaxActivations() != null ? license.getMaxActivations() : 0;
                capacity += Math.max(max - used, 0);
            }
            if (capacity >= required) {
                break;
            }
            if (batch.size() < size) {
                break;
            }
            page += 1;
        }
        return collected;
    }

    private List<License> resolveSelectedLicenses(UUID softwareId, List<UUID> selectedLicenseIds) {
        if (selectedLicenseIds == null || selectedLicenseIds.isEmpty()) {
            return List.of();
        }
        List<License> licenses = licenseRepository.findAllById(selectedLicenseIds);
        if (licenses.size() != selectedLicenseIds.size()) {
            throw new ResourceNotFoundException("One or more selected licenses were not found.");
        }
        for (License license : licenses) {
            if (license.getSoftware() == null || !softwareId.equals(license.getSoftware().getId())) {
                throw new BadRequestException("Selected license does not belong to the requested software.");
            }
            if (license.getStatus() != com.dismal.distribuciones.modules.store.domain.LicenseStatus.ACTIVE) {
                throw new BadRequestException("Selected license is not active.");
            }
            int used = license.getUsedActivations() != null ? license.getUsedActivations() : 0;
            int max = license.getMaxActivations() != null ? license.getMaxActivations() : 0;
            if (used >= max) {
                throw new InsufficientStockException("Selected license does not have available activations.");
            }
        }
        return licenses;
    }

    private int consumeLicenseForSale(License license, User client, int remaining,
                                      Map<UUID, License> updates, List<License> activationAssignments) {
        int used = license.getUsedActivations() != null ? license.getUsedActivations() : 0;
        int max = license.getMaxActivations() != null ? license.getMaxActivations() : 0;
        int available = Math.max(max - used, 0);
        if (available <= 0 || remaining <= 0) {
            return 0;
        }
        int consume = Math.min(available, remaining);
        license.setUsedActivations(used + consume);
        license.setOwner(client);
        updates.put(license.getId(), license);
        for (int i = 0; i < consume; i += 1) {
            activationAssignments.add(license);
        }
        return consume;
    }

    private void validateSelectedLicenses(com.dismal.distribuciones.modules.sales.dto.SaleItemRequest item) {
        if (item.selectedLicenseIds() == null || item.selectedLicenseIds().isEmpty()) {
            return;
        }
        Set<UUID> uniqueIds = new HashSet<>(item.selectedLicenseIds());
        if (uniqueIds.size() != item.selectedLicenseIds().size()) {
            throw new BadRequestException("Selected licenses contain duplicates.");
        }
    }

    private void validateRequestedLicenseSelections(List<com.dismal.distribuciones.modules.sales.dto.SaleItemRequest> items) {
        Set<UUID> selectedIds = new HashSet<>();
        for (com.dismal.distribuciones.modules.sales.dto.SaleItemRequest item : items) {
            if (item == null || item.selectedLicenseIds() == null) {
                continue;
            }
            for (UUID licenseId : item.selectedLicenseIds()) {
                if (!selectedIds.add(licenseId)) {
                    throw new BadRequestException("The same selected license cannot be used in more than one sale item.");
                }
            }
        }
    }

    private List<com.dismal.distribuciones.modules.sales.dto.SaleItemRequest> toSaleItemRequests(Sale sale) {
        return sale.getItems().stream()
                .map(item -> new com.dismal.distribuciones.modules.sales.dto.SaleItemRequest(
                        item.getSoftware().getId(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSelectedLicenseIdList()
                ))
                .toList();
    }

    private void releaseFulfillments(Sale sale) {
        List<SaleFulfillment> fulfillments = saleFulfillmentRepository.findBySaleIdWithLicense(sale.getId());
        if (fulfillments.isEmpty()) {
            return;
        }
        List<License> licenses = new ArrayList<>();
        for (SaleFulfillment fulfillment : fulfillments) {
            License license = fulfillment.getLicense();
            if (license == null) {
                continue;
            }
            int used = license.getUsedActivations() != null ? license.getUsedActivations() : 0;
            int updated = Math.max(used - 1, 0);
            license.setUsedActivations(updated);
            if (updated == 0) {
                license.setOwner(null);
            }
            licenses.add(license);
        }
        if (!licenses.isEmpty()) {
            licenseRepository.saveAll(licenses);
        }
        saleFulfillmentRepository.deleteBySaleId(sale.getId());
    }

    private void revertFinancialsOnCancel(Sale sale, AccountsReceivable ar) {
        if (sale.getSaleType() == SaleType.CREDIT && ar != null && ar.getStatus() != AccountsReceivableStatus.PAID) {
            releaseCredit(sale);
        }
        accountsReceivableRepository.deleteBySaleId(sale.getId());
        paymentRepository.deleteBySaleId(sale.getId());
        invoiceRepository.deleteBySaleId(sale.getId());
    }

    private void releaseCredit(Sale sale) {
        if (sale.getCustomerMarket() != null) {
            CustomerMarket market = customerMarketService.lock(sale.getCustomerMarket());
            BigDecimal newCreditUsed = safeBigDecimal(market.getCreditUsed()).subtract(sale.getTotal());
            market.setCreditUsed(newCreditUsed.max(BigDecimal.ZERO));
            customerMarketService.saveCredit(market);
            return;
        }
        User client = sale.getClient();
        BigDecimal newCreditUsed = safeBigDecimal(client.getCreditUsed()).subtract(sale.getTotal());
        client.setCreditUsed(newCreditUsed.max(BigDecimal.ZERO));
        userRepository.save(client);
    }

    private BigDecimal calculateActivationCost(License license) {
        BigDecimal purchasePrice = license.getPurchasePrice() != null ? license.getPurchasePrice() : BigDecimal.ZERO;
        Integer maxActivations = license.getMaxActivations();
        if (maxActivations == null || maxActivations <= 0) {
            return purchasePrice;
        }
        return purchasePrice.divide(BigDecimal.valueOf(maxActivations), 4, RoundingMode.HALF_UP);
    }

    private SaleResponse toSaleResponse(Sale sale) {
        BigDecimal paid = paymentRepository.sumAmountBySaleId(sale.getId());
        return toSaleResponse(sale, paid);
    }

    private SaleResponse toSaleResponse(Sale sale, BigDecimal paid) {
        BigDecimal balance = sale.getTotal().subtract(paid);
        if (balance.signum() < 0) {
            balance = BigDecimal.ZERO;
        }
        List<SaleItemResponse> items = sale.getItems().stream()
                .map(item -> new SaleItemResponse(
                        item.getId(),
                        item.getSoftware().getId(),
                        item.getSoftware().getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()
                ))
                .toList();
        return new SaleResponse(
                sale.getId(),
                SaleNumberService.format(sale.getCountry(), sale.getSaleNumber()),
                sale.getClient().getId(),
                sale.getSaleType(),
                sale.getCountry(),
                sale.getCurrency(),
                sale.getStatus(),
                sale.getTotal(),
                paid,
                balance,
                sale.getCreatedAt(),
                sale.getUpdatedAt(),
                items
        );
    }

    private void assignSaleNumber(Sale sale) {
        if (sale.getSaleNumber() == null) {
            sale.setSaleNumber(saleNumberService.nextNumber(sale.getCountry()));
        }
    }

    private Map<UUID, BigDecimal> getPaidBySaleIds(List<Sale> sales) {
        Map<UUID, BigDecimal> map = new HashMap<>();
        List<UUID> ids = sales.stream().map(Sale::getId).toList();
        if (ids.isEmpty()) {
            return map;
        }
        List<Object[]> rows = paymentRepository.sumAmountsBySaleIds(ids);
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            UUID saleId = safeUuid(row[0]);
            if (saleId == null) {
                continue;
            }
            BigDecimal sum = row[1] instanceof BigDecimal decimal ? decimal : BigDecimal.valueOf(((Number) row[1]).doubleValue());
            map.put(saleId, sum);
        }
        return map;
    }

    private void incrementTargetsForSale(Sale sale, int direction) {
        if (sale == null || sale.getItems() == null || sale.getItems().isEmpty()) {
            return;
        }
        for (SaleItem item : sale.getItems()) {
            if (item == null || item.getSoftware() == null) {
                continue;
            }
            int qty = item.getQuantity() != null ? item.getQuantity() : 0;
            if (qty <= 0) {
                continue;
            }
            salesTargetRepository.incrementUnitsSoldCurrent(item.getSoftware().getId(), qty * direction);
        }
    }

    private UUID safeUuid(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String string) {
            try {
                return UUID.fromString(string);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private BigDecimal safeBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
