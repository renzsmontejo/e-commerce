package com.revs.ecommerce.repositories;

import com.revs.ecommerce.models.Users;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface UsersRepository extends JpaRepository<Users, Long> {
    @EntityGraph(attributePaths = "roles")
    Optional<Users> findWithRolesById(Long id);
    Optional<Users> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

}
