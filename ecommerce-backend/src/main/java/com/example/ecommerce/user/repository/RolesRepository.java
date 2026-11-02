package com.example.ecommerce.user.repository;

import com.example.ecommerce.user.model.Roles;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolesRepository extends JpaRepository<Roles, Long> {
    Optional<Roles> findByName(String name);
}
