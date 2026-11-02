package com.example.ecommerce.product.repository;

import com.example.ecommerce.product.model.ProductAttributeAssignments;
import com.example.ecommerce.product.model.ids.ProductAttributeAssignmentsId;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductAttributeAssignmentsRepository extends JpaRepository<ProductAttributeAssignments, ProductAttributeAssignmentsId> {
}
