package com.revs.ecommerce.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariants {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MANY PRODUCTVARIANTS RECORDS RELATE TO PRODUCTS THROUGH PRODUCT_ID
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Products product;

    @Column(name = "sku", nullable = false, unique = true, length = 100)
    private String sku;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "compare_at_price", nullable = true, precision = 12, scale = 2)
    private BigDecimal compareAtPrice;

    @Column(name = "weight_grams", nullable = true)
    private Integer weightGrams;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // INVERSE RELATIONSHIP TO PRODUCTIMAGES
    @OneToMany(mappedBy = "variant", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ProductImages> images = new ArrayList<>();

    // INVERSE RELATIONSHIP TO CARTITEMS
    @OneToMany(mappedBy = "variant", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<CartItems> cartItems = new ArrayList<>();

    // INVERSE RELATIONSHIP TO ORDERITEMS
    @OneToMany(mappedBy = "variant", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<OrderItems> orderItems = new ArrayList<>();

    // INVERSE RELATIONSHIP TO VARIANTATTRIBUTEVALUES
    @OneToMany(mappedBy = "variant", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<VariantAttributeValues> attributeValues = new ArrayList<>();

    // INVERSE RELATIONSHIP TO INVENTORIES
    @OneToOne(mappedBy = "variant", fetch = FetchType.LAZY)
    @JsonIgnore
    private Inventories inventory;

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
