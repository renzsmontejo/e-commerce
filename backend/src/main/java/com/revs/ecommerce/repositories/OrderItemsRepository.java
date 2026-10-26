package com.revs.ecommerce.repositories;

import com.revs.ecommerce.models.OrderItems;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemsRepository extends JpaRepository<OrderItems, Long> {
}
