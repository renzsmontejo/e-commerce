package com.example.ecommerce.cart.repository;

import com.example.ecommerce.cart.model.Carts;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CartsRepository extends JpaRepository<Carts, Long> {
    java.util.Optional<Carts> findByUser_Id(Long userId);
}
