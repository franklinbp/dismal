package com.dismal.distribuciones.modules.integrations.outbox.repository;

import com.dismal.distribuciones.modules.integrations.outbox.domain.EventOutbox;
import com.dismal.distribuciones.modules.integrations.outbox.domain.EventOutboxStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventOutboxRepository extends JpaRepository<EventOutbox, UUID>, JpaSpecificationExecutor<EventOutbox> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EventOutbox e " +
            "where e.status in :statuses " +
            "and (e.nextAttemptAt is null or e.nextAttemptAt <= :now) " +
            "order by e.createdAt asc")
    List<EventOutbox> findReadyEvents(@Param("statuses") List<EventOutboxStatus> statuses,
                                      @Param("now") Instant now,
                                      Pageable pageable);

    long countByStatus(EventOutboxStatus status);

    List<EventOutbox> findTop20ByStatusOrderByCreatedAtDesc(EventOutboxStatus status);

    List<EventOutbox> findByAggregateId(UUID aggregateId);

    void deleteByAggregateId(UUID aggregateId);

    @Query("select e from EventOutbox e " +
            "where e.status = :status " +
            "and (:from is null or e.createdAt >= :from) " +
            "and (:to is null or e.createdAt <= :to) " +
            "order by e.createdAt desc")
    Page<EventOutbox> findByStatusAndCreatedAtRange(@Param("status") EventOutboxStatus status,
                                                    @Param("from") Instant from,
                                                    @Param("to") Instant to,
                                                    Pageable pageable);
}
