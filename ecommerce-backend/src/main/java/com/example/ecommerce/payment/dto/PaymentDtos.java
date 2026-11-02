package com.example.ecommerce.payment.dto;
import jakarta.validation.constraints.*;
public final class PaymentDtos {
    private PaymentDtos() {}
    public record CheckoutRequest(@NotNull @Positive Long orderId) {}
    public record SessionResponse(Long paymentId, Long orderId, String provider, String checkoutUrl) {}
    public record WebhookResponse(boolean received) {}
}
