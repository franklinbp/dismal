package com.dismal.distribuciones.modules.storefront.repository;

import com.dismal.distribuciones.modules.storefront.domain.StorefrontOrder;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontOrderStatus;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface StorefrontOrderRepository extends JpaRepository<StorefrontOrder, UUID> {
    boolean existsByOrderNumber(String orderNumber);

    Optional<StorefrontOrder> findByOrderNumber(String orderNumber);

    Optional<StorefrontOrder> findByPaymentProviderAndPaymentReference(String paymentProvider, String paymentReference);

    Optional<StorefrontOrder> findByPaymentAccountIdAndPaymentClaimReference(UUID paymentAccountId, String paymentClaimReference);

    Page<StorefrontOrder> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    Page<StorefrontOrder> findByCustomerIdAndCountryOrderByCreatedAtDesc(
            UUID customerId,
            StorefrontCountry country,
            Pageable pageable
    );

    Page<StorefrontOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<StorefrontOrder> findByStatusOrderByCreatedAtAsc(StorefrontOrderStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from StorefrontOrder o left join fetch o.items where o.orderNumber = :orderNumber")
    Optional<StorefrontOrder> findByOrderNumberForUpdate(@Param("orderNumber") String orderNumber);
}
