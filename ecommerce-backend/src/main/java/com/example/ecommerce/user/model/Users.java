package com.example.ecommerce.user.model;

import com.example.ecommerce.auth.model.AuthTokens;
import com.example.ecommerce.cart.model.Carts;
import com.example.ecommerce.cart.model.CouponUsages;
import com.example.ecommerce.order.model.Orders;
import com.example.ecommerce.order.model.OrderStatusHistory;
import com.example.ecommerce.inventory.model.InventoryMovements;
import com.example.ecommerce.review.model.ProductReviews;
import com.example.ecommerce.report.model.AuditLogs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 150)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 150)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "contact", nullable = true, length = 50)
    private String phone;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "email_verified_at", nullable = true)
    private LocalDateTime emailVerifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // MANY USERS CAN HAVE MANY ROLES
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    @JsonIgnore
    private Set<Roles> roles = new HashSet<>();

    // INVERSE RELATIONSHIP TO ADDRESSES
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Addresses> addresses = new ArrayList<>();

    // INVERSE RELATIONSHIP TO AUTHTOKENS
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<AuthTokens> authTokens = new ArrayList<>();

    // INVERSE RELATIONSHIP TO CARTS
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonIgnore
    private Carts cart;

    // INVERSE RELATIONSHIP TO WISHLISTS
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonIgnore
    private Wishlists wishlist;

    // INVERSE RELATIONSHIP TO ORDERS
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Orders> orders = new ArrayList<>();

    // INVERSE RELATIONSHIP TO INVENTORYMOVEMENTS
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<InventoryMovements> inventoryMovements = new ArrayList<>();

    // INVERSE RELATIONSHIP TO COUPONUSAGES
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<CouponUsages> couponUsages = new ArrayList<>();

    // INVERSE RELATIONSHIP TO PRODUCTREVIEWS
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ProductReviews> reviews = new ArrayList<>();

    // INVERSE RELATIONSHIP TO AUDITLOGS
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<AuditLogs> auditLogs = new ArrayList<>();

    // INVERSE RELATIONSHIP TO ORDERSTATUSHISTORY
    @OneToMany(mappedBy = "changedByUser", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<OrderStatusHistory> orderStatusChanges = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
