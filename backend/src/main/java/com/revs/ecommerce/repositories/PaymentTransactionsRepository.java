package com.revs.ecommerce.repositories;

import com.revs.ecommerce.models.PaymentTransactions;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionsRepository extends JpaRepository<PaymentTransactions, Long> {
}
