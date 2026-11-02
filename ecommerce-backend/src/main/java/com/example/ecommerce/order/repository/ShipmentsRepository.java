package com.example.ecommerce.order.repository;

import com.example.ecommerce.order.model.Shipments;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentsRepository extends JpaRepository<Shipments, Long> {
}
