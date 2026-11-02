package com.example.ecommerce.user.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "wishlists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Wishlists {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ONE WISHLISTS RECORDS RELATE TO USERS THROUGH USER_ID
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    private Users user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // INVERSE RELATIONSHIP TO WISHLISTITEMS
    @OneToMany(mappedBy = "wishlist", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<WishlistItems> items = new ArrayList<>();

    public void addItem(WishlistItems item) { items.add(item); item.setWishlist(this); }
    public void removeItem(WishlistItems item) { items.remove(item); item.setWishlist(null); }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

}
