package com.example.ecommerce.order.dto;

import com.example.ecommerce.order.dto.CheckoutDtos.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class OrderDtos {
    private OrderDtos() {}
    public enum Status { PENDING, PAID, PROCESSING, SHIPPED, DELIVERED, CANCELLATION_REQUESTED, CANCELLED }
    public record StatusRequest(@NotNull Status status, @Size(max=1000) String notes) {}
    public record CancellationRequest(@NotBlank @Size(max=1000) String reason) {}
    public record Summary(Long id, String orderNumber, String status, BigDecimal totalAmount, String currency, LocalDateTime placedAt) {}
    public record OrderPage(List<Summary> content, int page, int size, long totalElements, int totalPages) {}
    public record PaymentSummary(Long id, String method, String provider, String status, BigDecimal amount, String currency, LocalDateTime paidAt) {}
    public record StatusEvent(String previousStatus, String newStatus, String notes, LocalDateTime changedAt) {}
    public record Details(Long id, String orderNumber, String status, String recipientName, String shippingPhone,
        String shippingAddress, LocalDateTime placedAt, Totals totals, List<OrderLine> items,
        List<PaymentSummary> payments, List<StatusEvent> statusHistory) {}
}
