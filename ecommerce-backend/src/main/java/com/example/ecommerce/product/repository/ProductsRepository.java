package com.example.ecommerce.product.repository;

import com.example.ecommerce.product.model.Products;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductsRepository extends JpaRepository<Products, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Products> {
    Optional<Products> findBySlug(String slug);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Products p where p.id = :id")
    Optional<Products> findForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

}
