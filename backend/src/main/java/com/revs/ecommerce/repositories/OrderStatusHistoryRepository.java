package com.revs.ecommerce.repositories;

import com.revs.ecommerce.models.OrderStatusHistory;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
}
