package com.example.ecommerce.user.repository;

import com.example.ecommerce.user.model.WishlistItems;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistItemsRepository extends JpaRepository<WishlistItems, Long> {
}
