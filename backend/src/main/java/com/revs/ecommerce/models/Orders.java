package com.revs.ecommerce.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Orders {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MANY ORDERS RECORDS RELATE TO USERS THROUGH USER_ID
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private Users user;

    // MANY ORDERS RECORDS RELATE TO COUPONS THROUGH COUPON_ID
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "coupon_id", nullable = true)
    @JsonIgnore
    private Coupons coupon;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "shipping_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "PHP";

    @Column(name = "recipient_name", nullable = false, length = 150)
    private String recipientName;

    @Column(name = "shipping_phone", nullable = false, length = 50)
    private String shippingPhone;

    @Column(name = "shipping_address", nullable = false, columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "placed_at", nullable = false, updatable = false)
    private LocalDateTime placedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // INVERSE RELATIONSHIP TO ORDERITEMS
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JsonIgnore
    private List<OrderItems> items = new ArrayList<>();

    // INVERSE RELATIONSHIP TO PAYMENTS
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Payments> payments = new ArrayList<>();

    // INVERSE RELATIONSHIP TO SHIPMENTS
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Shipments> shipments = new ArrayList<>();

    // INVERSE RELATIONSHIP TO ORDERSTATUSHISTORY
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();

    // INVERSE RELATIONSHIP TO INVENTORYMOVEMENTS
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<InventoryMovements> inventoryMovements = new ArrayList<>();

    // INVERSE RELATIONSHIP TO COUPONUSAGES
    @OneToOne(mappedBy = "order", fetch = FetchType.LAZY)
    @JsonIgnore
    private CouponUsages couponUsage;

    public void addItem(OrderItems item) { items.add(item); item.setOrder(this); }

    @PrePersist
    protected void onCreate() {
        if (placedAt == null) placedAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
