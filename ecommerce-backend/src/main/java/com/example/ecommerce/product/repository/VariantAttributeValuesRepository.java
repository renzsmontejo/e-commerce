package com.example.ecommerce.product.repository;

import com.example.ecommerce.product.model.VariantAttributeValues;
import com.example.ecommerce.product.model.ids.VariantAttributeValuesId;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VariantAttributeValuesRepository extends JpaRepository<VariantAttributeValues, VariantAttributeValuesId> {
}
