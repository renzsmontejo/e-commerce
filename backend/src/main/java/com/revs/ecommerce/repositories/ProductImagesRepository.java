package com.revs.ecommerce.repositories;

import com.revs.ecommerce.models.ProductImages;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImagesRepository extends JpaRepository<ProductImages, Long> {
}
