package com.dismal.distribuciones.modules.sales.service;

import com.dismal.distribuciones.exception.BadRequestException;
import com.dismal.distribuciones.exception.ResourceNotFoundException;
import com.dismal.distribuciones.modules.sales.domain.Quote;
import com.dismal.distribuciones.modules.sales.domain.QuoteItem;
import com.dismal.distribuciones.modules.sales.domain.QuoteStatus;
import com.dismal.distribuciones.modules.sales.domain.SaleType;
import com.dismal.distribuciones.modules.sales.dto.CreateQuoteRequest;
import com.dismal.distribuciones.modules.sales.dto.CreateSaleRequest;
import com.dismal.distribuciones.modules.sales.dto.QuoteItemRequest;
import com.dismal.distribuciones.modules.sales.dto.QuoteItemResponse;
import com.dismal.distribuciones.modules.sales.dto.QuoteResponse;
import com.dismal.distribuciones.modules.sales.dto.QuoteSummaryResponse;
import com.dismal.distribuciones.modules.sales.dto.SaleItemRequest;
import com.dismal.distribuciones.modules.sales.dto.SaleResponse;
import com.dismal.distribuciones.modules.sales.repository.QuoteRepository;
import com.dismal.distribuciones.modules.sales.repository.SaleRepository;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.repository.UserRepository;
import com.dismal.distribuciones.modules.store.domain.Software;
import com.dismal.distribuciones.modules.store.repository.SoftwareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final UserRepository userRepository;
    private final SoftwareRepository softwareRepository;
    private final SaleRepository saleRepository;
    private final SalesService salesService;

    @Transactional
    public QuoteResponse createQuote(CreateQuoteRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new BadRequestException("Quote must include at least one item.");
        }
        User client = userRepository.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with ID: " + request.clientId()));

        Quote quote = new Quote();
        quote.setClient(client);
        quote.setStatus(QuoteStatus.DRAFT);
        quote.setNotes(request.notes());

        List<QuoteItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (QuoteItemRequest itemRequest : request.items()) {
            if (itemRequest.quantity() == null || itemRequest.quantity() <= 0) {
                throw new BadRequestException("Quote item quantity must be greater than zero.");
            }
            Software software = softwareRepository.findById(itemRequest.softwareId())
                    .orElseThrow(() -> new ResourceNotFoundException("Software not found with ID: " + itemRequest.softwareId()));

            BigDecimal unitPrice = itemRequest.unitPrice() != null ? itemRequest.unitPrice() : software.getPrice();
            if (unitPrice == null) {
                throw new BadRequestException("Quote item unit price is required.");
            }
            if (unitPrice.signum() < 0) {
                throw new BadRequestException("Quote item unit price cannot be negative.");
            }

            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.quantity()));
            QuoteItem item = new QuoteItem();
            item.setQuote(quote);
            item.setSoftware(software);
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(unitPrice);
            item.setSubtotal(subtotal);
            items.add(item);
            total = total.add(subtotal);
        }

        quote.setItems(items);
        quote.setTotal(total);
        Quote saved = quoteRepository.save(quote);
        return toQuoteResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<QuoteSummaryResponse> listQuotes(QuoteStatus status, int page, int size) {
        Specification<Quote> spec = Specification.where(null);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return quoteRepository.findAll(spec, PageRequest.of(page, size))
                .map(this::toQuoteSummaryResponse);
    }

    @Transactional(readOnly = true)
    public QuoteResponse getQuote(UUID id) {
        Quote quote = quoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quote not found with ID: " + id));
        return toQuoteResponse(quote);
    }

    @Transactional
    public SaleResponse convertToSale(UUID quoteId) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new ResourceNotFoundException("Quote not found with ID: " + quoteId));

        if (quote.getSale() != null) {
            return salesService.getSale(quote.getSale().getId());
        }

        List<SaleItemRequest> items = quote.getItems().stream()
                .map(item -> new SaleItemRequest(
                        item.getSoftware().getId(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        null
                ))
                .toList();
        CreateSaleRequest saleRequest = new CreateSaleRequest(
                quote.getClient().getId(),
                SaleType.CASH,
                items
        );
        SaleResponse saleResponse = salesService.createSale(saleRequest);
        quote.setSale(saleRepository.findById(saleResponse.id())
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with ID: " + saleResponse.id())));
        quote.setStatus(QuoteStatus.APPROVED);
        quoteRepository.save(quote);
        return saleResponse;
    }

    private QuoteResponse toQuoteResponse(Quote quote) {
        String clientName = quote.getClient().getFirstname() + " " + quote.getClient().getLastname();
        UUID saleId = quote.getSale() != null ? quote.getSale().getId() : null;
        return new QuoteResponse(
                quote.getId(),
                quote.getClient().getId(),
                clientName,
                quote.getClient().getEmail(),
                quote.getStatus(),
                quote.getTotal(),
                quote.getNotes(),
                saleId,
                quote.getCreatedAt(),
                quote.getUpdatedAt(),
                quote.getItems().stream().map(this::toQuoteItemResponse).toList()
        );
    }

    private QuoteSummaryResponse toQuoteSummaryResponse(Quote quote) {
        String clientName = quote.getClient().getFirstname() + " " + quote.getClient().getLastname();
        return new QuoteSummaryResponse(
                quote.getId(),
                quote.getClient().getId(),
                clientName,
                quote.getClient().getEmail(),
                quote.getStatus(),
                quote.getTotal(),
                quote.getCreatedAt(),
                quote.getUpdatedAt()
        );
    }

    private QuoteItemResponse toQuoteItemResponse(QuoteItem item) {
        return new QuoteItemResponse(
                item.getId(),
                item.getSoftware().getId(),
                item.getSoftware().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()
        );
    }
}
