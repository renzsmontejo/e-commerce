package com.example.ecommerce.product.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "product_attributes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttributes {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    // INVERSE RELATIONSHIP TO PRODUCTATTRIBUTEVALUES
    @OneToMany(mappedBy = "attribute", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ProductAttributeValues> values = new ArrayList<>();

    // INVERSE RELATIONSHIP TO PRODUCTATTRIBUTEASSIGNMENTS
    @OneToMany(mappedBy = "attribute", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ProductAttributeAssignments> productAssignments = new ArrayList<>();

}
