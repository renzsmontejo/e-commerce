package com.example.ecommerce.user.model;

import com.example.ecommerce.product.model.Products;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "wishlist_items", uniqueConstraints = {@UniqueConstraint(columnNames = {"wishlist_id", "product_id"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItems {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MANY WISHLISTITEMS RECORDS RELATE TO WISHLISTS THROUGH WISHLIST_ID
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wishlist_id", nullable = false)
    @JsonIgnore
    private Wishlists wishlist;

    // MANY WISHLISTITEMS RECORDS RELATE TO PRODUCTS THROUGH PRODUCT_ID
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Products product;

    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @PrePersist
    protected void onCreate() {
        if (addedAt == null) addedAt = LocalDateTime.now();
    }

}
