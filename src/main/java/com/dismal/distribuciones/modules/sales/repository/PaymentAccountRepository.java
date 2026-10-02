package com.dismal.distribuciones.modules.sales.repository;

import com.dismal.distribuciones.modules.sales.domain.PaymentAccount;
import com.dismal.distribuciones.modules.sales.domain.PaymentAccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentAccountRepository extends JpaRepository<PaymentAccount, UUID> {

    List<PaymentAccount> findAllByOrderByActiveDescDefaultAccountDescNameAsc();

    List<PaymentAccount> findByActiveTrueOrderByDefaultAccountDescNameAsc();

    List<PaymentAccount> findByActiveTrueAndPublicForStorefrontTrueOrderByDefaultAccountDescNameAsc();

    Optional<PaymentAccount> findFirstByDefaultAccountTrueAndActiveTrueOrderByNameAsc();

    Optional<PaymentAccount> findFirstByTypeAndActiveTrueOrderByDefaultAccountDescNameAsc(PaymentAccountType type);
}
