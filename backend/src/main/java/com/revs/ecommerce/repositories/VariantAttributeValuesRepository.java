package com.revs.ecommerce.repositories;

import com.revs.ecommerce.models.VariantAttributeValues;
import com.revs.ecommerce.models.ids.VariantAttributeValuesId;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VariantAttributeValuesRepository extends JpaRepository<VariantAttributeValues, VariantAttributeValuesId> {
}
