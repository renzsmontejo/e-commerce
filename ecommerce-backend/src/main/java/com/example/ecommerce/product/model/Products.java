package com.example.ecommerce.product.model;

import com.example.ecommerce.user.model.WishlistItems;
import com.example.ecommerce.category.model.Categories;
import com.example.ecommerce.review.model.ProductReviews;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Products {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MANY PRODUCTS RECORDS RELATE TO CATEGORIES THROUGH CATEGORY_ID
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    @JsonIgnore
    private Categories category;

    // MANY PRODUCTS RECORDS RELATE TO BRANDS THROUGH BRAND_ID
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "brand_id", nullable = true)
    @JsonIgnore
    private Brands brand;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    @Column(name = "description", nullable = true, columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // INVERSE RELATIONSHIP TO PRODUCTIMAGES
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ProductImages> images = new ArrayList<>();

    // INVERSE RELATIONSHIP TO PRODUCTVARIANTS
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ProductVariants> variants = new ArrayList<>();

    // INVERSE RELATIONSHIP TO WISHLISTITEMS
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<WishlistItems> wishlistItems = new ArrayList<>();

    // INVERSE RELATIONSHIP TO PRODUCTREVIEWS
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ProductReviews> reviews = new ArrayList<>();

    // INVERSE RELATIONSHIP TO PRODUCTATTRIBUTEASSIGNMENTS
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ProductAttributeAssignments> attributeAssignments = new ArrayList<>();

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
