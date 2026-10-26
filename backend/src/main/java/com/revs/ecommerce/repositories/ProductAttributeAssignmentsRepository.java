package com.revs.ecommerce.repositories;

import com.revs.ecommerce.models.ProductAttributeAssignments;
import com.revs.ecommerce.models.ids.ProductAttributeAssignmentsId;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductAttributeAssignmentsRepository extends JpaRepository<ProductAttributeAssignments, ProductAttributeAssignmentsId> {
}
