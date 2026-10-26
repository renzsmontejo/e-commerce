package com.revs.ecommerce.repositories;

import com.revs.ecommerce.models.Brands;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandsRepository extends JpaRepository<Brands, Long> {
}
