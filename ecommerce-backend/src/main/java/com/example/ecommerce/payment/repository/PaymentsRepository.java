package com.example.ecommerce.payment.repository;

import com.example.ecommerce.payment.model.Payments;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentsRepository extends JpaRepository<Payments, Long> {
    java.util.List<Payments> findByOrder_IdOrderByIdAsc(Long orderId);
    boolean existsByOrder_IdAndStatusIn(Long orderId, java.util.Collection<String> statuses);
    java.util.Optional<Payments> findByProviderReference(String reference);
    long countByOrder_Id(Long orderId);
}
