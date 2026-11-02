package com.example.ecommerce.order.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "shipments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Shipments {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MANY SHIPMENTS RECORDS RELATE TO ORDERS THROUGH ORDER_ID
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Orders order;

    @Column(name = "courier", nullable = true, length = 100)
    private String courier;

    @Column(name = "tracking_number", nullable = true, length = 150)
    private String trackingNumber;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "shipped_at", nullable = true)
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at", nullable = true)
    private LocalDateTime deliveredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

}
