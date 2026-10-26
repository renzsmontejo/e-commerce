package com.revs.ecommerce.repositories;

import com.revs.ecommerce.models.CouponUsages;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponUsagesRepository extends JpaRepository<CouponUsages, Long> {
    boolean existsByOrderId(Long orderId);
    long countByCouponId(Long couponId);

}
