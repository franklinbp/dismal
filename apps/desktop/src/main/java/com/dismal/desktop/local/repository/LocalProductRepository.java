package com.dismal.desktop.local.repository;

import com.dismal.desktop.local.domain.LocalProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalProductRepository extends JpaRepository<LocalProduct, String> {
}
