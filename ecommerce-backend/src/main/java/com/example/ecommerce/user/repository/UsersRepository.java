package com.example.ecommerce.user.repository;

import com.example.ecommerce.user.model.Users;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface UsersRepository extends JpaRepository<Users, Long> {
    @EntityGraph(attributePaths = "roles")
    Optional<Users> findWithRolesById(Long id);
    Optional<Users> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    // All cart mutations and checkout serialize on the owner, including first-cart creation.
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from Users u where u.id = :id")
    Optional<Users> findForCartUpdate(@org.springframework.data.repository.query.Param("id") Long id);
}
