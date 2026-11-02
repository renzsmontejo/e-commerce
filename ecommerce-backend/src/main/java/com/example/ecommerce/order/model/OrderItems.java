package com.example.ecommerce.order.model;

import com.example.ecommerce.product.model.ProductVariants;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItems {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MANY ORDERITEMS RECORDS RELATE TO ORDERS THROUGH ORDER_ID
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Orders order;

    // MANY ORDERITEMS RECORDS RELATE TO PRODUCTVARIANTS THROUGH VARIANT_ID
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "variant_id", nullable = true)
    @JsonIgnore
    private ProductVariants variant;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @Column(name = "variant_description", nullable = true, length = 500)
    private String variantDescription;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    @PrePersist
    protected void validateLine() {
        if (quantity == null || quantity <= 0 || unitPrice == null || unitPrice.signum() < 0) throw new IllegalStateException("Invalid order item quantity or price");
        lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

}
