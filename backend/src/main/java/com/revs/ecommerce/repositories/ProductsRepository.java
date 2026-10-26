package com.revs.ecommerce.repositories;

import com.revs.ecommerce.models.Products;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductsRepository extends JpaRepository<Products, Long> {
    Optional<Products> findBySlug(String slug);

}
