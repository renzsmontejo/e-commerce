package com.revs.ecommerce.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "product_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviews {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MANY PRODUCTREVIEWS RECORDS RELATE TO PRODUCTS THROUGH PRODUCT_ID
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Products product;

    // MANY PRODUCTREVIEWS RECORDS RELATE TO USERS THROUGH USER_ID
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private Users user;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "title", nullable = true, length = 200)
    private String title;

    @Column(name = "comment", nullable = true, columnDefinition = "TEXT")
    private String comment;

    @Column(name = "is_approved", nullable = false)
    private boolean isApproved = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private void validateRating() { if (rating == null || rating < 1 || rating > 5) throw new IllegalStateException("Rating must be 1 to 5"); }

    @PrePersist
    protected void onCreate() {
        validateRating();
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        validateRating();
        updatedAt = LocalDateTime.now();
    }

}
