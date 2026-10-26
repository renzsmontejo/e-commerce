package com.revs.ecommerce.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "coupon_usages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CouponUsages {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MANY COUPONUSAGES RECORDS RELATE TO COUPONS THROUGH COUPON_ID
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id", nullable = false)
    @JsonIgnore
    private Coupons coupon;

    // MANY COUPONUSAGES RECORDS RELATE TO USERS THROUGH USER_ID
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private Users user;

    // ONE COUPONUSAGES RECORDS RELATE TO ORDERS THROUGH ORDER_ID
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    @JsonIgnore
    private Orders order;

    @Column(name = "used_at", nullable = false, updatable = false)
    private LocalDateTime usedAt;

    @PrePersist
    protected void onCreate() {
        if (usedAt == null) usedAt = LocalDateTime.now();
    }

}
