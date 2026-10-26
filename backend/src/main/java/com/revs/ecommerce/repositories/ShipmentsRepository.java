package com.revs.ecommerce.repositories;

import com.revs.ecommerce.models.Shipments;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentsRepository extends JpaRepository<Shipments, Long> {
}
