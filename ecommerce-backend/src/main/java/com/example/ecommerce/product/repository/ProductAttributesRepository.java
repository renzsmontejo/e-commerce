package com.example.ecommerce.product.repository;

import com.example.ecommerce.product.model.ProductAttributes;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductAttributesRepository extends JpaRepository<ProductAttributes, Long> {
}
