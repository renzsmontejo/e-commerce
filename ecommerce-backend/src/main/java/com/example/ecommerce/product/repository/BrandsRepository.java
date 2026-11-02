package com.example.ecommerce.product.repository;

import com.example.ecommerce.product.model.Brands;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandsRepository extends JpaRepository<Brands, Long> {
}
