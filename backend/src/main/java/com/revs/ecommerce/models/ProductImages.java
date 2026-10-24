package com.revs.ecommerce.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "product_images")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProductImages {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MANY IMAGE BELONGS TO ONE PRODUCT
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Products products;

    // MANY IMAGE BELONGS TO ONE PRODUCT VARIANT
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variants_id")
    private ProductVariants productVariants;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @Column(name = "is_primary")
    private Boolean isPrimary;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
