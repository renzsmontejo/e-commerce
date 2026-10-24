package com.revs.ecommerce.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Categories {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MANY SUB CATEGORIES BELONG TO ONE CATEGORY
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Categories parent;

    // ONE CATEGORY HAS MANY SUBCATEGORY
    @OneToMany(mappedBy = "parent")
    private List<Categories> children = new ArrayList<>();

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "slug", unique = true, length = 180)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;


    // ONE CATEGORY HAS MANY PRODUCTS
    @OneToMany(mappedBy = "categories")
    private List<Products> products = new ArrayList<>();


    @PrePersist
    protected void onCreate(){
        if(createdAt == null){
            createdAt = LocalDateTime.now();
        }
    }
}
