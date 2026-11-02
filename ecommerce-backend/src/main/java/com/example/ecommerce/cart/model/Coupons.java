package com.example.ecommerce.cart.model;

import com.example.ecommerce.order.model.Orders;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Coupons {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "discount_type", nullable = false, length = 20)
    private String discountType;

    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "minimum_order_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal minimumOrderAmount = BigDecimal.ZERO;

    @Column(name = "usage_limit", nullable = true)
    private Integer usageLimit;

    @Column(name = "starts_at", nullable = true)
    private LocalDateTime startsAt;

    @Column(name = "expires_at", nullable = true)
    private LocalDateTime expiresAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // INVERSE RELATIONSHIP TO ORDERS
    @OneToMany(mappedBy = "coupon", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Orders> orders = new ArrayList<>();

    // INVERSE RELATIONSHIP TO COUPONUSAGES
    @OneToMany(mappedBy = "coupon", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<CouponUsages> usages = new ArrayList<>();

    // OPTIMISTIC LOCK VERSION
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Transient
    public boolean isCurrentlyValid() {
        LocalDateTime now = LocalDateTime.now();
        return isActive && (startsAt == null || !now.isBefore(startsAt))
            && (expiresAt == null || now.isBefore(expiresAt));
    }
    @Transient
    public boolean meetsMinimumOrderAmount(BigDecimal subtotal) {
        return subtotal != null && minimumOrderAmount != null && subtotal.compareTo(minimumOrderAmount) >= 0;
    }
    @Transient
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        if (subtotal == null || subtotal.signum() <= 0 || !isCurrentlyValid() || !meetsMinimumOrderAmount(subtotal)) return BigDecimal.ZERO;
        if (discountValue == null || discountValue.signum() <= 0) throw new IllegalStateException("Invalid discount value");
        BigDecimal amount;
        if ("PERCENTAGE".equals(discountType)) {
            if (discountValue.compareTo(BigDecimal.valueOf(100)) > 0) throw new IllegalStateException("Percentage exceeds 100");
            amount = subtotal.multiply(discountValue).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        } else if ("FIXED".equals(discountType)) { amount = discountValue; }
        else throw new IllegalStateException("Unsupported discount type");
        return amount.min(subtotal);
    }

}
