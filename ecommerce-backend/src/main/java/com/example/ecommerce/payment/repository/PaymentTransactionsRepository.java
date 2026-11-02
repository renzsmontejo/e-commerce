package com.example.ecommerce.payment.repository;

import com.example.ecommerce.payment.model.PaymentTransactions;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionsRepository extends JpaRepository<PaymentTransactions, Long> {
    @org.springframework.data.jpa.repository.Query(value="select id from payment_transactions where payment_id=:paymentId and provider_transaction_id=:eventId for update",nativeQuery=true)
    java.util.Optional<Long> findEventForUpdate(@org.springframework.data.repository.query.Param("paymentId") Long paymentId,
            @org.springframework.data.repository.query.Param("eventId") String eventId);
}
