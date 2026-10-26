package com.revs.ecommerce.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "cart_items", uniqueConstraints = {@UniqueConstraint(columnNames = {"cart_id", "variant_id"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItems {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MANY CARTITEMS RECORDS RELATE TO CARTS THROUGH CART_ID
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    @JsonIgnore
    private Carts cart;

    // MANY CARTITEMS RECORDS RELATE TO PRODUCTVARIANTS THROUGH VARIANT_ID
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    @JsonIgnore
    private ProductVariants variant;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @PrePersist
    protected void onCreate() {
        if (addedAt == null) addedAt = LocalDateTime.now();
    }

}
