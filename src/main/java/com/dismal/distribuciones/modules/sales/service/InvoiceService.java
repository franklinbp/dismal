package com.dismal.distribuciones.modules.sales.service;

import com.dismal.distribuciones.exception.BadRequestException;
import com.dismal.distribuciones.exception.ResourceNotFoundException;
import com.dismal.distribuciones.modules.sales.domain.AccountsReceivable;
import com.dismal.distribuciones.modules.sales.domain.Invoice;
import com.dismal.distribuciones.modules.sales.domain.InvoiceStatus;
import com.dismal.distribuciones.modules.sales.domain.Sale;
import com.dismal.distribuciones.modules.sales.domain.SaleStatus;
import com.dismal.distribuciones.modules.sales.dto.CreateInvoiceRequest;
import com.dismal.distribuciones.modules.sales.dto.InvoiceResponse;
import com.dismal.distribuciones.modules.sales.dto.InvoiceSummaryResponse;
import com.dismal.distribuciones.modules.sales.repository.AccountsReceivableRepository;
import com.dismal.distribuciones.modules.sales.repository.InvoiceRepository;
import com.dismal.distribuciones.modules.sales.repository.SaleRepository;
import com.dismal.distribuciones.modules.integrations.outbox.service.OutboxService;
import com.dismal.distribuciones.modules.security.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private static final DateTimeFormatter DATE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final InvoiceRepository invoiceRepository;
    private final SaleRepository saleRepository;
    private final AccountsReceivableRepository accountsReceivableRepository;
    private final OutboxService outboxService;

    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {
        if (request.saleId() == null) {
            throw new BadRequestException("saleId is required.");
        }
        Sale sale = saleRepository.findWithItemsById(request.saleId())
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with ID: " + request.saleId()));

        if (!(sale.getStatus() == SaleStatus.CONFIRMED || sale.getStatus() == SaleStatus.PAID)) {
            throw new BadRequestException("Only confirmed sales can be invoiced.");
        }
        invoiceRepository.findBySaleId(sale.getId()).ifPresent(existing -> {
            throw new BadRequestException("Invoice already exists for sale " + sale.getId());
        });

        Invoice invoice = new Invoice();
        invoice.setSale(sale);
        invoice.setStatus(sale.getStatus() == SaleStatus.PAID ? InvoiceStatus.PAID : InvoiceStatus.PENDING);
        invoice.setTotalAmount(sale.getTotal());

        LocalDate issueDate = LocalDate.now();
        LocalDate dueDate = resolveDueDate(request.dueDate(), sale, issueDate);
        invoice.setDueDate(dueDate);

        Invoice saved = saveWithInvoiceNumberRetry(invoice, issueDate);
        AccountsReceivable ar = accountsReceivableRepository.findBySaleId(sale.getId()).orElse(null);
        outboxService.enqueueInvoiceIssuedEvent(
                saved,
                sale,
                ar != null ? ar.getDueDate() : null,
                ar != null ? ar.getBalance() : BigDecimal.ZERO
        );
        return toInvoiceResponse(saved, sale.getClient());
    }

    @Transactional(readOnly = true)
    public Page<InvoiceSummaryResponse> listInvoices(String q, LocalDate from, LocalDate to, int page, int size) {
        Specification<Invoice> spec = Specification.where(null);
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("issueDate"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("issueDate"), to));
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                var saleJoin = root.join("sale", jakarta.persistence.criteria.JoinType.LEFT);
                var orderJoin = root.join("order", jakarta.persistence.criteria.JoinType.LEFT);
                var saleClient = saleJoin.join("client", jakarta.persistence.criteria.JoinType.LEFT);
                var orderCustomer = orderJoin.join("customer", jakarta.persistence.criteria.JoinType.LEFT);

                var invoiceNumberLike = cb.like(cb.lower(root.get("invoiceNumber")), like);
                var saleEmailLike = cb.like(cb.lower(saleClient.get("email")), like);
                var orderEmailLike = cb.like(cb.lower(orderCustomer.get("email")), like);
                var saleNameLike = cb.like(cb.lower(cb.concat(
                        cb.coalesce(saleClient.get("firstname"), ""),
                        cb.concat(" ", cb.coalesce(saleClient.get("lastname"), ""))
                )), like);
                var orderNameLike = cb.like(cb.lower(cb.concat(
                        cb.coalesce(orderCustomer.get("firstname"), ""),
                        cb.concat(" ", cb.coalesce(orderCustomer.get("lastname"), ""))
                )), like);

                return cb.or(invoiceNumberLike, saleEmailLike, orderEmailLike, saleNameLike, orderNameLike);
            });
        }

        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        return invoiceRepository.findAll(spec, pageRequest)
                .map(this::toInvoiceSummaryResponse);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with ID: " + id));
        User client = resolveClient(invoice);
        return toInvoiceResponse(invoice, client);
    }

    @Transactional
    public InvoiceResponse voidInvoice(UUID id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with ID: " + id));
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BadRequestException("Cannot void a paid invoice.");
        }
        invoice.setStatus(InvoiceStatus.CANCELLED);
        Invoice saved = invoiceRepository.save(invoice);
        return toInvoiceResponse(saved, resolveClient(saved));
    }

    private String nextInvoiceNumber(LocalDate issueDate) {
        String prefix = "INV-" + DATE_STAMP.format(issueDate) + "-";
        return invoiceRepository.findTopByInvoiceNumberStartingWithOrderByInvoiceNumberDesc(prefix)
                .map(Invoice::getInvoiceNumber)
                .map(number -> incrementInvoiceNumber(prefix, number))
                .orElse(prefix + "0001");
    }

    private String incrementInvoiceNumber(String prefix, String current) {
        String sequence = current.substring(prefix.length());
        int value;
        try {
            value = Integer.parseInt(sequence);
        } catch (NumberFormatException ex) {
            value = 0;
        }
        return prefix + String.format("%04d", value + 1);
    }

    private Invoice saveWithInvoiceNumberRetry(Invoice invoice, LocalDate issueDate) {
        int attempts = 0;
        while (attempts < 3) {
            attempts++;
            invoice.setInvoiceNumber(nextInvoiceNumber(issueDate));
            try {
                return invoiceRepository.save(invoice);
            } catch (DataIntegrityViolationException ex) {
                // Retry in case another transaction took the same invoice number.
                if (attempts >= 3) {
                    throw ex;
                }
            }
        }
        throw new IllegalStateException("Unable to generate unique invoice number");
    }

    private LocalDate resolveDueDate(LocalDate requested, Sale sale, LocalDate issueDate) {
        if (requested != null) {
            return requested;
        }
        AccountsReceivable ar = accountsReceivableRepository.findBySaleId(sale.getId()).orElse(null);
        if (ar != null && ar.getDueDate() != null) {
            return ar.getDueDate();
        }
        return issueDate;
    }

    private User resolveClient(Invoice invoice) {
        if (invoice.getSale() != null) {
            return invoice.getSale().getClient();
        }
        if (invoice.getOrder() != null) {
            return invoice.getOrder().getCustomer();
        }
        throw new BadRequestException("Invoice is missing customer reference.");
    }

    private InvoiceResponse toInvoiceResponse(Invoice invoice, User client) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getStatus(),
                invoice.getIssueDate(),
                invoice.getDueDate(),
                invoice.getTotalAmount(),
                invoice.getSale() != null ? invoice.getSale().getId() : null,
                client.getId(),
                client.getFirstname() + " " + client.getLastname(),
                client.getEmail()
        );
    }

    private InvoiceSummaryResponse toInvoiceSummaryResponse(Invoice invoice) {
        User client = resolveClient(invoice);
        return new InvoiceSummaryResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getStatus(),
                invoice.getIssueDate(),
                invoice.getDueDate(),
                invoice.getTotalAmount(),
                invoice.getSale() != null ? invoice.getSale().getId() : null,
                client.getFirstname() + " " + client.getLastname(),
                client.getEmail()
        );
    }
}
