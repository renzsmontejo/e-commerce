package com.revs.ecommerce.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    // MANY PRODUCT VARIANTS BELONG TO ONE PRODUCT
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Products products;

    // ONE PRODUCT VARIANT HAS MANY IMAGES
    @OneToMany(mappedBy = "productVariants")
    private List<ProductImages> productImages = new ArrayList<>();

    // ONE PRODUCT VARIANT HAS MANY INVENTORY MOVEMENT
    @OneToMany(mappedBy = "productVariants")
    private List<InventoryMovements> inventoryMovements = new ArrayList<>();


    @Column(name = "sku", length = 100, unique = true, nullable = false)
    private String sku;

    @Column(name = "variant_name", length = 50)
    private String variantName;

    @Column(name="color", nullable = true)
    private String color;

    @Column(name = "size", nullable = false)
    private String size;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "compare_at_price", nullable = true)
    private BigDecimal compareAtPrice;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold;

    @Column(name = "status", length = 20)
    private String status;

    @JsonFormat(pattern = "MM-dd-yyyy HH:mm:ss")
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate(){
        if(createdAt == null){
            createdAt = LocalDateTime.now();
        }
    }
}
