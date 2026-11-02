package com.example.ecommerce.order.dto;

import com.example.ecommerce.cart.dto.CartDtos.ItemResponse;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class CheckoutDtos {
    private CheckoutDtos() {}
    public record PlaceOrderRequest(
        @NotBlank @Size(max=150) String recipientName,
        @NotBlank @Size(max=50) String shippingPhone,
        @NotBlank @Size(max=2000) String shippingAddress,
        @NotNull @DecimalMin("0") @Digits(integer=10,fraction=2) BigDecimal expectedTotal) {}
    public record Totals(BigDecimal subtotal, BigDecimal discountAmount, BigDecimal shippingFee,
        BigDecimal taxAmount, BigDecimal totalAmount, String currency) {}
    public record PreviewResponse(List<ItemResponse> items, Totals totals) {}
    public record OrderLine(Long id, Long variantId, String productName, String sku, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {}
    public record OrderResponse(Long id, String orderNumber, String status, String recipientName, String shippingPhone,
        String shippingAddress, LocalDateTime placedAt, Totals totals, List<OrderLine> items) {}
}
