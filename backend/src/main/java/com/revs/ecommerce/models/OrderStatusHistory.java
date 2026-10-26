package com.revs.ecommerce.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "order_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MANY ORDERSTATUSHISTORY RECORDS RELATE TO ORDERS THROUGH ORDER_ID
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Orders order;

    // MANY ORDERSTATUSHISTORY RECORDS RELATE TO USERS THROUGH CHANGED_BY_USER_ID
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "changed_by_user_id", nullable = true)
    @JsonIgnore
    private Users changedByUser;

    @Column(name = "previous_status", nullable = true, length = 30)
    private String previousStatus;

    @Column(name = "new_status", nullable = false, length = 30)
    private String newStatus;

    @Column(name = "notes", nullable = true, columnDefinition = "TEXT")
    private String notes;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        if (changedAt == null) changedAt = LocalDateTime.now();
    }

}
