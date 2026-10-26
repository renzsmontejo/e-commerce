package com.revs.ecommerce.models.ids;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class ProductAttributeAssignmentsId implements Serializable {
    private static final long serialVersionUID = 1L;
    @Column(name = "product_id")
    private Long productId;
    @Column(name = "attribute_id")
    private Long attributeId;
}
