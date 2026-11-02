package com.example.ecommerce.product.repository;

import com.example.ecommerce.product.model.ProductImages;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImagesRepository extends JpaRepository<ProductImages, Long> {
}
