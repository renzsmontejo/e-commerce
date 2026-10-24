package com.revs.ecommerce.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="inventory_movements")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class InventoryMovements {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MANY INVENTORY MOVEMENT BELONGS TO ONE PRODUCT VARIANT
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name= "variant_id")
    private ProductVariants productVariants;

    // MANY INVENTORY MOVEMENT BELONGS TO ONE USER
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private Users users;

    @Column(name = "quantity_change")
    private Integer quantityChange;

    @Column(name = "reason", length = 100)
    private String reason;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referencedId;

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
