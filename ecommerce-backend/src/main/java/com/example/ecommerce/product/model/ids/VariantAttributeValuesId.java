package com.example.ecommerce.product.model.ids;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class VariantAttributeValuesId implements Serializable {
    private static final long serialVersionUID = 1L;
    @Column(name = "variant_id")
    private Long variantId;
    @Column(name = "attribute_value_id")
    private Long attributeValueId;
}
