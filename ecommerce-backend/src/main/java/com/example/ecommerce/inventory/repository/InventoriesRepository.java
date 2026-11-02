package com.example.ecommerce.inventory.repository;

import com.example.ecommerce.inventory.model.Inventories;

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

    Optional<Inventories> findByVariant_Id(Long variantId);
    // Lock the row without hydrating an entity whose cached version may predate another checkout.
    @Query(value="select id from inventories where variant_id = :variantId for update", nativeQuery=true)
    Optional<Long> lockIdByVariantId(@Param("variantId") Long variantId);
}
