package com.dismal.distribuciones.modules.store.repository;

import com.dismal.distribuciones.modules.store.domain.SalesGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SalesGoalRepository extends JpaRepository<SalesGoal, UUID> {

    /**
     * Finds all sales goals associated with a specific software product.
     * @param softwareId The UUID of the software product.
     * @return A list of sales goals for the given software.
     */
    List<SalesGoal> findBySoftwareId(UUID softwareId);
}
