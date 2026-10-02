package com.dismal.distribuciones.modules.sales.controller;

import com.dismal.distribuciones.modules.sales.domain.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sales")
public class PublicSalesController {

    public record PurchaseRequest(UUID softwareId) {}

    @PostMapping("/purchase")
    public ResponseEntity<Order> purchase(@RequestBody PurchaseRequest request) {
        return ResponseEntity.status(HttpStatus.GONE).build();
    }
}
