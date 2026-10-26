package com.revs.ecommerce.repositories;

import com.revs.ecommerce.models.Carts;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CartsRepository extends JpaRepository<Carts, Long> {
}
