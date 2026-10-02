package com.dismal.distribuciones.modules.marketing.repository;

import com.dismal.distribuciones.modules.marketing.domain.Campaign;
import com.dismal.distribuciones.modules.marketing.domain.CampaignStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, UUID>, JpaSpecificationExecutor<Campaign> {

    /**
     * Finds all campaigns that are in a SCHEDULED state and whose scheduled time is now or in the past.
     * These are the campaigns that are ready to be processed and sent by the sending engine.
     * @param status The status to filter by (e.g., CampaignStatus.SCHEDULED).
     * @param now The current time, passed to the query.
     * @return A list of campaigns ready to be sent.
     */
    @Query("SELECT c FROM Campaign c WHERE c.status = :status AND c.scheduledAt <= :now")
    List<Campaign> findPendingCampaignsToSend(
            @Param("status") CampaignStatus status,
            @Param("now") LocalDateTime now
    );

    @Override
    @EntityGraph(attributePaths = {"product"})
    Page<Campaign> findAll(org.springframework.data.jpa.domain.Specification<Campaign> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"product"})
    Optional<Campaign> findById(UUID id);

    long countByStatus(CampaignStatus status);
}
