package com.example.ecommerce.auth.repository;
import com.example.ecommerce.auth.model.AuthTokens;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface AuthTokensRepository extends JpaRepository<AuthTokens, Long> {
    Optional<AuthTokens> findByTokenHashAndTokenType(String hash, String type);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from AuthTokens t where t.tokenHash = :hash and t.tokenType = 'PASSWORD_RESET'")
    Optional<AuthTokens> findResetForUpdate(@org.springframework.data.repository.query.Param("hash") String hash);
    @Modifying
    @Query("update AuthTokens t set t.revokedAt = CURRENT_TIMESTAMP where t.user.id = :userId and t.revokedAt is null")
    void revokeAllForUser(@org.springframework.data.repository.query.Param("userId") Long userId);
}
