package com.revs.ecommerce.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "inventory_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InventoryMovements {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MANY INVENTORYMOVEMENTS RECORDS RELATE TO INVENTORIES THROUGH INVENTORY_ID
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_id", nullable = false)
    @JsonIgnore
    private Inventories inventory;

    // MANY INVENTORYMOVEMENTS RECORDS RELATE TO USERS THROUGH USER_ID
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id", nullable = true)
    @JsonIgnore
    private Users user;

    // MANY INVENTORYMOVEMENTS RECORDS RELATE TO ORDERS THROUGH ORDER_ID
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "order_id", nullable = true)
    @JsonIgnore
    private Orders order;

    @Column(name = "movement_type", nullable = false, length = 50)
    private String movementType;

    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;

    @Column(name = "reason", nullable = true, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (quantityChange == null || quantityChange == 0) throw new IllegalStateException("Movement quantity must not be zero");
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

}
