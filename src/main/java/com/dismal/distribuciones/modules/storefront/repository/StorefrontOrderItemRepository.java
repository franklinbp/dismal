package com.dismal.distribuciones.modules.storefront.repository;

import com.dismal.distribuciones.modules.storefront.domain.StorefrontOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StorefrontOrderItemRepository extends JpaRepository<StorefrontOrderItem, UUID> {
}
