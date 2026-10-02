package com.dismal.distribuciones.modules.sales.service;

import com.dismal.distribuciones.exception.BadRequestException;
import com.dismal.distribuciones.exception.ResourceNotFoundException;
import com.dismal.distribuciones.modules.sales.domain.*;
import com.dismal.distribuciones.modules.sales.dto.PaymentRequest;
import com.dismal.distribuciones.modules.sales.dto.PaymentResponse;
import com.dismal.distribuciones.modules.sales.dto.SaleResponse;
import com.dismal.distribuciones.modules.sales.repository.AccountsReceivableRepository;
import com.dismal.distribuciones.modules.sales.repository.InvoiceRepository;
import com.dismal.distribuciones.modules.sales.repository.PaymentRepository;
import com.dismal.distribuciones.modules.sales.repository.SaleRepository;
import com.dismal.distribuciones.modules.integrations.outbox.service.OutboxService;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.customer.domain.CustomerMarket;
import com.dismal.distribuciones.modules.security.customer.service.CustomerMarketService;
import com.dismal.distribuciones.modules.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final SaleRepository saleRepository;
    private final PaymentRepository paymentRepository;
    private final AccountsReceivableRepository accountsReceivableRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final SalesService salesService;
    private final OutboxService outboxService;
    private final PaymentAccountService paymentAccountService;
    private final CustomerMarketService customerMarketService;

    @Transactional
    public SaleResponse addPayment(PaymentRequest request) {
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        Sale sale = saleRepository.findWithItemsById(request.saleId())
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with ID: " + request.saleId()));

        if (sale.getStatus() == SaleStatus.CANCELLED) {
            throw new IllegalStateException("Cannot add payment to a cancelled sale");
        }
        if (sale.getStatus() == SaleStatus.PAID) {
            throw new BadRequestException("Sale is already paid");
        }
        if (sale.getStatus() != SaleStatus.CONFIRMED) {
            throw new BadRequestException("Sale must be confirmed before adding payments");
        }

        PaymentAccount paymentAccount = paymentAccountService.resolveForPayment(request.paymentAccountId());
        Payment payment = Payment.builder()
                .sale(sale)
                .amount(request.amount())
                .method(request.method() != null ? request.method() : PaymentMethod.CASH)
                .reference(request.reference())
                .paymentAccount(paymentAccount)
                .build();
        paymentRepository.save(payment);

        BigDecimal paid = paymentRepository.sumAmountBySaleId(sale.getId());
        AccountsReceivable ar = accountsReceivableRepository.findBySaleId(sale.getId()).orElse(null);
        if (paid.compareTo(sale.getTotal()) >= 0) {
            if (ar != null) {
                ar = settleAccountsReceivable(sale, ar);
            }
            sale.setStatus(SaleStatus.PAID);
            saleRepository.save(sale);
            invoiceRepository.findBySaleId(sale.getId()).ifPresent(invoice -> {
                invoice.setStatus(InvoiceStatus.PAID);
                invoiceRepository.save(invoice);
            });
        } else if (ar != null) {
            ar = updateAccountsReceivablePartial(sale, ar, paid);
        }

        if (request.shouldNotifyClient()) {
            enqueuePaymentReceivedEventSafely(sale, payment, ar);
        }

        return salesService.getSale(sale.getId());
    }

    private void enqueuePaymentReceivedEventSafely(Sale sale, Payment payment, AccountsReceivable ar) {
        try {
            if (ar != null) {
                outboxService.enqueuePaymentReceivedEvent(sale, payment, ar.getDueDate(), ar.getBalance());
            } else {
                outboxService.enqueuePaymentReceivedEvent(sale, payment, null, BigDecimal.ZERO);
            }
        } catch (RuntimeException ex) {
            log.warn(
                    "Payment was registered but payment notification event could not be queued. saleId={} paymentId={} error={}",
                    sale.getId(),
                    payment.getId(),
                    ex.getMessage(),
                    ex
            );
        }
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listPayments(UUID saleId) {
        return paymentRepository.findBySaleIdOrderByCreatedAtAsc(saleId).stream()
                .map(payment -> new PaymentResponse(
                        payment.getId(),
                        payment.getSale().getId(),
                        payment.getAmount(),
                        payment.getMethod(),
                        payment.getReference(),
                        payment.getPaymentAccount() != null ? payment.getPaymentAccount().getId() : null,
                        payment.getPaymentAccount() != null ? payment.getPaymentAccount().getName() : null,
                        payment.getCreatedAt()
                ))
                .toList();
    }

    private AccountsReceivable settleAccountsReceivable(Sale sale, AccountsReceivable ar) {
        ar.setPaid(sale.getTotal());
        ar.setBalance(BigDecimal.ZERO);
        ar.setStatus(AccountsReceivableStatus.PAID);
        ar = accountsReceivableRepository.save(ar);

        if (sale.getSaleType() == SaleType.CREDIT) {
            if (sale.getCustomerMarket() != null) {
                CustomerMarket market = customerMarketService.lock(sale.getCustomerMarket());
                BigDecimal newCreditUsed = safeBigDecimal(market.getCreditUsed()).subtract(sale.getTotal());
                market.setCreditUsed(newCreditUsed.max(BigDecimal.ZERO));
                customerMarketService.saveCredit(market);
                return ar;
            }
            User client = sale.getClient();
            BigDecimal creditUsed = safeBigDecimal(client.getCreditUsed());
            BigDecimal newCreditUsed = creditUsed.subtract(sale.getTotal());
            if (newCreditUsed.signum() < 0) {
                newCreditUsed = BigDecimal.ZERO;
            }
            client.setCreditUsed(newCreditUsed);
            userRepository.save(client);
        }
        return ar;
    }

    private AccountsReceivable updateAccountsReceivablePartial(Sale sale, AccountsReceivable ar, BigDecimal paid) {
        BigDecimal balance = sale.getTotal().subtract(paid);
        if (balance.signum() < 0) {
            balance = BigDecimal.ZERO;
        }
        ar.setPaid(paid);
        ar.setBalance(balance);
        return accountsReceivableRepository.save(ar);
    }

    // License assignment occurs on sale confirmation (not payment).

    private BigDecimal safeBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
