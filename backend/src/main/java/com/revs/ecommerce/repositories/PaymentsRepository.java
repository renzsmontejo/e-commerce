package com.revs.ecommerce.repositories;

import com.revs.ecommerce.models.Payments;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentsRepository extends JpaRepository<Payments, Long> {
}
