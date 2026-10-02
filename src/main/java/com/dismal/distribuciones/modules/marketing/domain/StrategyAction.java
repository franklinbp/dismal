package com.dismal.distribuciones.modules.marketing.domain;

import com.dismal.distribuciones.modules.marketing.intelligence.domain.MarketingPriority;
import com.dismal.distribuciones.modules.planning.domain.SalesTarget;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.store.domain.Software;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "strategy_actions")
public class StrategyAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Software product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id")
    private SalesTarget target;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StrategyActionSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MarketingPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StrategyActionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private StrategyActionChannel recommendedChannel;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    private LocalDate dueDate;

    private LocalDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    private String resultNotes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
