package com.example.ecommerce.category.repository;

import com.example.ecommerce.category.model.Categories;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriesRepository extends JpaRepository<Categories, Long> {
    java.util.List<Categories> findByIsActiveTrueOrderByNameAscIdAsc();
}
