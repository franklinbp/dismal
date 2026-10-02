package com.dismal.distribuciones.modules.sales.repository;

import com.dismal.distribuciones.modules.sales.domain.Quote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, UUID>, JpaSpecificationExecutor<Quote> {

    @Override
    @EntityGraph(attributePaths = {"client"})
    Page<Quote> findAll(org.springframework.data.jpa.domain.Specification<Quote> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"client", "items", "items.software", "sale"})
    Optional<Quote> findById(UUID id);
}
