package com.revs.ecommerce.repositories;

import com.revs.ecommerce.models.Inventories;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoriesRepository extends JpaRepository<Inventories, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventories i WHERE i.variant.id = :variantId")
    Optional<Inventories> findByVariantIdForUpdate(@Param("variantId") Long variantId);

}
