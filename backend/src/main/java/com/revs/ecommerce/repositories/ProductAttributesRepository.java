package com.revs.ecommerce.repositories;

import com.revs.ecommerce.models.ProductAttributes;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductAttributesRepository extends JpaRepository<ProductAttributes, Long> {
}
