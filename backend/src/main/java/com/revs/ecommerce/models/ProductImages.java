package com.revs.ecommerce.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "product_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductImages {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MANY PRODUCTIMAGES RECORDS RELATE TO PRODUCTS THROUGH PRODUCT_ID
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Products product;

    // MANY PRODUCTIMAGES RECORDS RELATE TO PRODUCTVARIANTS THROUGH VARIANT_ID
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "variant_id", nullable = true)
    @JsonIgnore
    private ProductVariants variant;

    @Column(name = "image_url", nullable = false, length = 1000)
    private String imageUrl;

    @Column(name = "alt_text", nullable = true, length = 255)
    private String altText;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = false;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

}
