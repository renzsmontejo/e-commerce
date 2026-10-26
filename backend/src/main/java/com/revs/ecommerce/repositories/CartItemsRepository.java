package com.revs.ecommerce.repositories;

import com.revs.ecommerce.models.CartItems;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemsRepository extends JpaRepository<CartItems, Long> {
}
