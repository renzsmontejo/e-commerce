package com.revs.ecommerce.repositories;

import com.revs.ecommerce.models.WishlistItems;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistItemsRepository extends JpaRepository<WishlistItems, Long> {
}
