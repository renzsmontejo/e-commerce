package com.revs.ecommerce.repositories;

import com.revs.ecommerce.models.InventoryMovements;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementsRepository extends JpaRepository<InventoryMovements, Long> {
}
