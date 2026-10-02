package com.dismal.distribuciones.modules.store.service;

import com.dismal.distribuciones.exception.BadRequestException;
import com.dismal.distribuciones.exception.ResourceNotFoundException;
import com.dismal.distribuciones.modules.store.domain.Software;
import com.dismal.distribuciones.modules.store.repository.SoftwareRepository;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PhysicalInventoryService {

    private final SoftwareRepository softwareRepository;

    public Software reserve(java.util.UUID productId, int quantity) {
        Software product = softwareRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productId));
        if (!Boolean.TRUE.equals(product.getPhysicalProduct())) {
            return product;
        }
        int stock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        int reserved = product.getReservedQuantity() != null ? product.getReservedQuantity() : 0;
        if (stock - reserved < quantity) {
            throw new BadRequestException("No hay existencias suficientes para " + product.getName() + ".");
        }
        product.setReservedQuantity(reserved + quantity);
        return softwareRepository.save(product);
    }

    public void release(StorefrontOrder order) {
        order.getItems().forEach(item -> adjustReservation(item.getSoftware().getId(), item.getQuantity(), false));
    }

    public void dispatch(StorefrontOrder order) {
        order.getItems().forEach(item -> adjustReservation(item.getSoftware().getId(), item.getQuantity(), true));
    }

    private void adjustReservation(java.util.UUID productId, int quantity, boolean deductStock) {
        Software product = softwareRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productId));
        if (!Boolean.TRUE.equals(product.getPhysicalProduct())) {
            return;
        }
        int reserved = Math.max(0, (product.getReservedQuantity() != null ? product.getReservedQuantity() : 0) - quantity);
        product.setReservedQuantity(reserved);
        if (deductStock) {
            int stock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
            product.setStockQuantity(Math.max(0, stock - quantity));
        }
        softwareRepository.save(product);
    }
}
