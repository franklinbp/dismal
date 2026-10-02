package com.dismal.desktop.local.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "local_products")
@AllArgsConstructor
@NoArgsConstructor
public class LocalProduct {
    @Id
    private String id; // UUID as String
    private String name;
    private BigDecimal price;
    private String platform;
    private String description;
}
