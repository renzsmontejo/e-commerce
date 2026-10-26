package com.revs.ecommerce.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.revs.ecommerce.models.ids.VariantAttributeValuesId;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "variant_attribute_values")
@Getter @Setter @NoArgsConstructor
public class VariantAttributeValues {
    @EmbeddedId
    private VariantAttributeValuesId id = new VariantAttributeValuesId();

    // MANY ASSIGNMENTS BELONG TO ONE PRODUCTVARIANTS
    @MapsId("variantId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    @JsonIgnore
    private ProductVariants variant;

    // MANY ASSIGNMENTS BELONG TO ONE PRODUCTATTRIBUTEVALUES
    @MapsId("attributeValueId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attribute_value_id", nullable = false)
    @JsonIgnore
    private ProductAttributeValues attributeValue;

    public VariantAttributeValues(ProductVariants variant, ProductAttributeValues attributeValue) {
        this.variant = variant;
        this.attributeValue = attributeValue;
    }
}
