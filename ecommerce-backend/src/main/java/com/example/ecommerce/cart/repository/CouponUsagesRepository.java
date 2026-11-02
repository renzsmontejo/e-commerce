package com.example.ecommerce.cart.repository;

import com.example.ecommerce.cart.model.CouponUsages;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponUsagesRepository extends JpaRepository<CouponUsages, Long> {
    boolean existsByOrderId(Long orderId);
    long countByCouponId(Long couponId);

}
