package com.revs.ecommerce.repositories;

import com.revs.ecommerce.models.Wishlists;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistsRepository extends JpaRepository<Wishlists, Long> {
}
