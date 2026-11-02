package com.example.ecommerce.inventory.repository;

import com.example.ecommerce.inventory.model.InventoryMovements;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementsRepository extends JpaRepository<InventoryMovements, Long> {
}
