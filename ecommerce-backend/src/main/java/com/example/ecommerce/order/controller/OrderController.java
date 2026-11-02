package com.example.ecommerce.order.controller;

import com.example.ecommerce.order.dto.OrderDtos.*;
import com.example.ecommerce.order.service.OrderService;
import com.example.ecommerce.user.model.Users;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
public class OrderController {
    private final OrderService orders;
    public OrderController(OrderService orders) { this.orders=orders; }
    @GetMapping("/api/orders")
    public OrderPage history(@AuthenticationPrincipal Users user,@RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size) { return orders.customerOrders(user.getId(),page,size); }
    @GetMapping("/api/orders/{id}")
    public Details details(@AuthenticationPrincipal Users user,@PathVariable @Positive Long id) { return orders.customerDetail(user.getId(),id); }
    @PostMapping("/api/orders/{id}/cancel")
    public Details cancel(@AuthenticationPrincipal Users user,@PathVariable @Positive Long id,@Valid @RequestBody CancellationRequest request) {
        return orders.requestCancellation(user.getId(),id,request.reason());
    }
    @GetMapping("/api/admin/orders")
    public OrderPage all(@RequestParam(required=false) Status status,@RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size) { return orders.adminOrders(status,page,size); }
    @PatchMapping("/api/admin/orders/{id}/status")
    public Details status(@AuthenticationPrincipal Users user,@PathVariable @Positive Long id,@Valid @RequestBody StatusRequest request) {
        return orders.changeStatus(user.getId(),id,request);
    }
}
