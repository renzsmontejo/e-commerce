package com.example.ecommerce.order.controller;

import com.example.ecommerce.order.dto.CheckoutDtos.*;
import com.example.ecommerce.order.service.CheckoutService;
import com.example.ecommerce.user.model.Users;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class CheckoutController {
    private final CheckoutService checkout;
    public CheckoutController(CheckoutService checkout) { this.checkout=checkout; }
    @PostMapping("/api/checkout/preview")
    public PreviewResponse preview(@AuthenticationPrincipal Users user) { return checkout.preview(user.getId()); }
    @PostMapping("/api/orders")
    public ResponseEntity<OrderResponse> place(@AuthenticationPrincipal Users user, @Valid @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(checkout.place(user.getId(),request));
    }
}
