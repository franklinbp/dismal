package com.dismal.desktop.local.repository;

import com.dismal.desktop.local.domain.LocalSalesTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalSalesTargetRepository extends JpaRepository<LocalSalesTarget, String> {
}
