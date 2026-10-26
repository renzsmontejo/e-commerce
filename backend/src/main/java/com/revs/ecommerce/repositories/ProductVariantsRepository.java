package com.revs.ecommerce.repositories;

import com.revs.ecommerce.models.ProductVariants;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantsRepository extends JpaRepository<ProductVariants, Long> {
    Optional<ProductVariants> findBySku(String sku);

}
