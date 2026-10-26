package com.revs.ecommerce.repositories;

import com.revs.ecommerce.models.Addresses;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressesRepository extends JpaRepository<Addresses, Long> {
}
