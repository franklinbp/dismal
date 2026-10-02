package com.dismal.distribuciones.modules.storefront.service;

import com.dismal.distribuciones.modules.sales.domain.PaymentMethod;
import com.dismal.distribuciones.modules.sales.domain.SaleType;
import com.dismal.distribuciones.modules.sales.dto.CreateSaleRequest;
import com.dismal.distribuciones.modules.sales.dto.PaymentRequest;
import com.dismal.distribuciones.modules.sales.dto.SaleItemRequest;
import com.dismal.distribuciones.modules.sales.dto.SaleResponse;
import com.dismal.distribuciones.modules.sales.service.PaymentService;
import com.dismal.distribuciones.modules.sales.service.SalesService;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontOrder;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontOrderStatus;
import com.dismal.distribuciones.modules.storefront.repository.StorefrontOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorefrontFulfillmentService {

    private final StorefrontOrderRepository storefrontOrderRepository;
    private final SalesService salesService;
    private final PaymentService paymentService;
    private final com.dismal.distribuciones.modules.store.service.PhysicalInventoryService physicalInventoryService;

    public StorefrontOrder fulfillCredit(StorefrontOrder order) {
        if (order.getSaleId() != null) {
            return order;
        }
        SaleResponse confirmedSale = createAndConfirmSale(order, SaleType.CREDIT);
        order.setPaymentProvider("CUSTOMER_CREDIT");
        order.setPaymentReference(order.getOrderNumber());
        order.setSaleId(confirmedSale.id());
        order.setStatus(StorefrontOrderStatus.PREPARING);
        return storefrontOrderRepository.save(order);
    }

    public StorefrontOrder fulfillPaid(
            StorefrontOrder order,
            String provider,
            String reference,
            BigDecimal amount,
            PaymentMethod method,
            UUID paymentAccountId,
            boolean notifyClient
    ) {
        if (order.getSaleId() != null) {
            return order;
        }
        SaleResponse confirmedSale = createAndConfirmSale(order, SaleType.CASH);
        paymentService.addPayment(new PaymentRequest(
                confirmedSale.id(),
                amount,
                method != null ? method : PaymentMethod.TRANSFER,
                provider + ":" + reference + " | " + order.getOrderNumber(),
                paymentAccountId,
                notifyClient
        ));

        order.setPaymentProvider(provider);
        order.setPaymentReference(reference);
        order.setPaymentAccountId(paymentAccountId);
        order.setSaleId(confirmedSale.id());
        order.setStatus(StorefrontOrderStatus.PREPARING);
        return storefrontOrderRepository.save(order);
    }

    private SaleResponse createAndConfirmSale(StorefrontOrder order, SaleType saleType) {
        SaleResponse createdSale = salesService.createSale(new CreateSaleRequest(
                order.getCustomer().getId(),
                saleType,
                order.getCountry(),
                toSaleItems(order)
        ));
        return salesService.confirmSale(createdSale.id());
    }

    private List<SaleItemRequest> toSaleItems(StorefrontOrder order) {
        return order.getItems().stream()
                .map(item -> new SaleItemRequest(
                        item.getSoftware().getId(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        null
                ))
                .toList();
    }
}
