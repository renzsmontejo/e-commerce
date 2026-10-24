package com.revs.ecommerce.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "products")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Products {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MANY PRODUCTS BELONG TO ONE CATEGORY
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Categories categories;

    // ONE PRODUCT HAS MANY VARIANTS
    @OneToMany(mappedBy = "products")
    private List<ProductVariants> productVariants = new ArrayList<>();

    // ONE PRODUCT HAS MANY IMAGES
    @OneToMany(mappedBy = "products")
    private List<ProductImages> productImages = new ArrayList<>();

    @Column(name = "slug", unique = true)
    private String slug;

    @Column(name="description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "brand", nullable = false, length = 100)
    private String brand;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "is_featured", nullable=false)
    private Boolean isFeatured = true;

    @JsonFormat(pattern = "MM-dd-yyyy HH:mm:ss")
    @Column(name = "created_at", updatable=false)
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "MM-dd-yyyy HH-mm-ss")
    @Column(name = "updated_at", updatable = true)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate(){
        LocalDateTime now = LocalDateTime.now();
        if(createdAt == null){
            createdAt = now;
        }
        if(updatedAt == null){
            updatedAt = now;
        }
    }
    @PreUpdate
    protected void onUpdate(){
        updatedAt = LocalDateTime.now();
    }

}
