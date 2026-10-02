package com.dismal.distribuciones.modules.store.repository;

import com.dismal.distribuciones.modules.store.domain.License;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LicenseRepository extends JpaRepository<License, UUID>, JpaSpecificationExecutor<License> {

    long countBySoftwareId(UUID softwareId);

    List<License> findBySoftwareId(UUID softwareId);

    void deleteBySoftwareId(UUID softwareId);

    @Query("select l.software.id, coalesce(sum(l.purchasePrice), 0), coalesce(sum(l.maxActivations), 0) " +
           "from License l group by l.software.id")
    List<Object[]> sumCostAndActivationsBySoftware();

    /**
     * Finds available licenses for a given software ID, ordered by their creation time.
     * An available license is one where the number of used activations is less than the maximum allowed activations.
     *
     * @param softwareId The UUID of the software.
     * @param pageable   A Pageable object to limit the results. To get just the next single available license,
     *                   you can use PageRequest.of(0, 1) in your service layer.
     * @return A list of available licenses.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM License l WHERE l.software.id = :softwareId " +
           "AND l.status = com.dismal.distribuciones.modules.store.domain.LicenseStatus.ACTIVE " +
           "AND l.usedActivations < l.maxActivations ORDER BY l.id ASC")
    List<License> findAvailableLicenses(@Param("softwareId") UUID softwareId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM License l WHERE l.software.id = :softwareId " +
           "AND l.status = com.dismal.distribuciones.modules.store.domain.LicenseStatus.ACTIVE " +
           "AND l.usedActivations < l.maxActivations ORDER BY l.id ASC")
    List<License> findAvailableUnassignedLicenses(@Param("softwareId") UUID softwareId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(CASE WHEN l.status = com.dismal.distribuciones.modules.store.domain.LicenseStatus.ACTIVE " +
           "AND l.usedActivations < l.maxActivations THEN (l.maxActivations - l.usedActivations) ELSE 0 END), 0) " +
           "FROM License l WHERE l.software.id = :softwareId")
    long countAvailableActivations(@Param("softwareId") UUID softwareId);

    @Query("SELECT l.software.id AS softwareId, l.software.name AS softwareName, " +
           "SUM(CASE WHEN l.status = com.dismal.distribuciones.modules.store.domain.LicenseStatus.ACTIVE " +
           "AND l.usedActivations < l.maxActivations THEN (l.maxActivations - l.usedActivations) ELSE 0 END) AS availableCount, " +
           "SUM(CASE WHEN l.usedActivations > 0 THEN l.usedActivations ELSE 0 END) AS assignedCount " +
           "FROM License l GROUP BY l.software.id, l.software.name")
    List<StockSummaryView> getStockSummary();
}
