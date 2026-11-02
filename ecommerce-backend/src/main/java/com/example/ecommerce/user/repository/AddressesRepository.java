package com.example.ecommerce.user.repository;

import com.example.ecommerce.user.model.Addresses;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressesRepository extends JpaRepository<Addresses, Long> {
}
