package com.revs.ecommerce.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "product_attribute_values", uniqueConstraints = {@UniqueConstraint(columnNames = {"attribute_id", "value"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttributeValues {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MANY PRODUCTATTRIBUTEVALUES RECORDS RELATE TO PRODUCTATTRIBUTES THROUGH ATTRIBUTE_ID
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attribute_id", nullable = false)
    @JsonIgnore
    private ProductAttributes attribute;

    @Column(name = "value", nullable = false, length = 100)
    private String value;

    // INVERSE RELATIONSHIP TO VARIANTATTRIBUTEVALUES
    @OneToMany(mappedBy = "attributeValue", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<VariantAttributeValues> variantAttributeValues = new ArrayList<>();

}
