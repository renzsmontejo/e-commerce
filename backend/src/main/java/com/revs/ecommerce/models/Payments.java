package com.revs.ecommerce.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payments {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MANY PAYMENTS RECORDS RELATE TO ORDERS THROUGH ORDER_ID
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Orders order;

    @Column(name = "method", nullable = false, length = 50)
    private String method;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "PHP";

    @Column(name = "provider", nullable = true, length = 100)
    private String provider;

    @Column(name = "provider_reference", nullable = true, unique = true, length = 255)
    private String providerReference;

    @Column(name = "paid_at", nullable = true)
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // INVERSE RELATIONSHIP TO PAYMENTTRANSACTIONS
    @OneToMany(mappedBy = "payment", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JsonIgnore
    private List<PaymentTransactions> transactions = new ArrayList<>();

    public void addTransaction(PaymentTransactions transaction) { transactions.add(transaction); transaction.setPayment(this); }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

}
