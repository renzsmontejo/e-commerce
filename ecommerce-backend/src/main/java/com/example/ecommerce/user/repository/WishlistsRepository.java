package com.example.ecommerce.user.repository;

import com.example.ecommerce.user.model.Wishlists;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistsRepository extends JpaRepository<Wishlists, Long> {
}
