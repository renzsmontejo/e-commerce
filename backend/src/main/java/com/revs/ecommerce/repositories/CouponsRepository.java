package com.revs.ecommerce.repositories;

import com.revs.ecommerce.models.Coupons;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponsRepository extends JpaRepository<Coupons, Long> {
    Optional<Coupons> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupons c WHERE c.code = :code")
    Optional<Coupons> findByCodeForUpdate(@Param("code") String code);

}
