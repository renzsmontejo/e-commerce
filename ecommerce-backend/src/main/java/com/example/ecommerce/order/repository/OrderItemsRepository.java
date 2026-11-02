package com.example.ecommerce.order.repository;

import com.example.ecommerce.order.model.OrderItems;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemsRepository extends JpaRepository<OrderItems, Long> {
}
