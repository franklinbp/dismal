package com.dismal.distribuciones.modules.marketing.repository;

import com.dismal.distribuciones.modules.marketing.domain.StrategyAction;
import com.dismal.distribuciones.modules.marketing.domain.StrategyActionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StrategyActionRepository extends JpaRepository<StrategyAction, UUID>, JpaSpecificationExecutor<StrategyAction> {

    @Override
    @EntityGraph(attributePaths = {"product", "target", "assignedTo"})
    List<StrategyAction> findAll();

    long countByStatusIn(List<StrategyActionStatus> statuses);
}
