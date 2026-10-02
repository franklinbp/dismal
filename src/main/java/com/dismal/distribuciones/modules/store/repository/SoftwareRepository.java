package com.dismal.distribuciones.modules.store.repository;

import com.dismal.distribuciones.modules.store.domain.Software;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import jakarta.persistence.LockModeType;

@Repository
public interface SoftwareRepository extends JpaRepository<Software, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Software s where s.id = :id")
    Optional<Software> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Finds software products that have available stock but have not been sold since a given date.
     * This is useful for identifying "cold stock" that may need a marketing push.
     * @param since The cut-off date.
     * @return A list of software products with stock but no recent sales.
     */
    @Query("SELECT s FROM Software s WHERE " +
           "EXISTS (SELECT 1 FROM License l WHERE l.software = s AND l.usedActivations < l.maxActivations) AND " +
           "s.id NOT IN (SELECT o.purchasedSoftware.id FROM Order o WHERE o.purchaseDate >= :since)")
    List<Software> findInStockSoftwareWithoutRecentSales(@Param("since") LocalDateTime since);
}

