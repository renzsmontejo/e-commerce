package com.example.ecommerce.product.repository;

import com.example.ecommerce.product.model.ProductVariants;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantsRepository extends JpaRepository<ProductVariants, Long> {
    Optional<ProductVariants> findBySku(String sku);

}
