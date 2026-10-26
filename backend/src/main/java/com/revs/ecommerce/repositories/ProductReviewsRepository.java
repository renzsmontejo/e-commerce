package com.revs.ecommerce.repositories;

import com.revs.ecommerce.models.ProductReviews;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductReviewsRepository extends JpaRepository<ProductReviews, Long> {
    List<ProductReviews> findByProductIdAndIsApprovedTrue(Long productId);
    List<ProductReviews> findByIsApprovedFalse();

}
