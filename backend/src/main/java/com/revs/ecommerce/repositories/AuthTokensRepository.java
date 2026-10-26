package com.revs.ecommerce.repositories;

import com.revs.ecommerce.models.AuthTokens;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthTokensRepository extends JpaRepository<AuthTokens, Long> {
}
