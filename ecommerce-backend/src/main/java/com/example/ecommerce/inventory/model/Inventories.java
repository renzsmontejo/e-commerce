package com.example.ecommerce.inventory.model;

import com.example.ecommerce.product.model.ProductVariants;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "inventories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Inventories {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ONE INVENTORIES RECORDS RELATE TO PRODUCTVARIANTS THROUGH VARIANT_ID
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false, unique = true)
    @JsonIgnore
    private ProductVariants variant;

    @Column(name = "quantity_on_hand", nullable = false)
    private Integer quantityOnHand = 0;

    @Column(name = "quantity_reserved", nullable = false)
    private Integer quantityReserved = 0;

    @Column(name = "reorder_level", nullable = false)
    private Integer reorderLevel = 5;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // INVERSE RELATIONSHIP TO INVENTORYMOVEMENTS
    @OneToMany(mappedBy = "inventory", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<InventoryMovements> movements = new ArrayList<>();

    // OPTIMISTIC LOCK VERSION
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Transient
    public int getAvailableQuantity() { return quantityOnHand - quantityReserved; }
    @Transient
    public boolean hasAvailableStock(int quantity) { return quantity > 0 && getAvailableQuantity() >= quantity; }
    @Transient
    public boolean needsReorder() { return getAvailableQuantity() <= reorderLevel; }

    @PrePersist
    protected void onCreate() {
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
