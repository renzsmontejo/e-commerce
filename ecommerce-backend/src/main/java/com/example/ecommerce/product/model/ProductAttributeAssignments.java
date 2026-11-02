package com.example.ecommerce.product.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.example.ecommerce.product.model.ids.ProductAttributeAssignmentsId;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_attribute_assignments")
@Getter @Setter @NoArgsConstructor
public class ProductAttributeAssignments {
    @EmbeddedId
    private ProductAttributeAssignmentsId id = new ProductAttributeAssignmentsId();

    // MANY ASSIGNMENTS BELONG TO ONE PRODUCTS
    @MapsId("productId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Products product;

    // MANY ASSIGNMENTS BELONG TO ONE PRODUCTATTRIBUTES
    @MapsId("attributeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attribute_id", nullable = false)
    @JsonIgnore
    private ProductAttributes attribute;

    public ProductAttributeAssignments(Products product, ProductAttributes attribute) {
        this.product = product;
        this.attribute = attribute;
    }
}
