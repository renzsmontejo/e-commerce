package com.example.ecommerce.cart.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public final class CartDtos {
    private CartDtos() {}
    public record AddItemRequest(@NotNull @Positive Long variantId, @NotNull @Min(1) @Max(999) Integer quantity) {}
    public record QuantityRequest(@NotNull @Min(1) @Max(999) Integer quantity) {}
    public record ItemResponse(Long id, Long productId, Long variantId, String productName, String sku,
            int quantity, BigDecimal unitPrice, BigDecimal lineTotal, int availableQuantity, boolean purchasable) {}
    public record CartResponse(Long id, List<ItemResponse> items, int totalQuantity, BigDecimal subtotal, String currency) {}
}
