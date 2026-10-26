package com.revs.ecommerce.repositories;

import com.revs.ecommerce.models.Categories;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriesRepository extends JpaRepository<Categories, Long> {
}
