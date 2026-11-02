package com.example.ecommerce.order.repository;

import com.example.ecommerce.order.model.Orders;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdersRepository extends JpaRepository<Orders, Long> {
    Optional<Orders> findByOrderNumber(String orderNumber);

    org.springframework.data.domain.Page<Orders> findByUser_Id(Long userId, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Orders> findByStatus(String status, org.springframework.data.domain.Pageable pageable);
    Optional<Orders> findByIdAndUser_Id(Long id, Long userId);
    @org.springframework.data.jpa.repository.Query(value="select id from orders where id = :id for update", nativeQuery=true)
    Optional<Long> lockId(@org.springframework.data.repository.query.Param("id") Long id);
}
