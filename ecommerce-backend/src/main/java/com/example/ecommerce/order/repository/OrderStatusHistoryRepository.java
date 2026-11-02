package com.example.ecommerce.order.repository;

import com.example.ecommerce.order.model.OrderStatusHistory;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
}
