package com.example.ecommerce.payment.controller;

import com.example.ecommerce.payment.dto.PaymentDtos.*;
import com.example.ecommerce.payment.service.PaymentService;
import com.example.ecommerce.user.model.Users;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

@RestController
public class PaymentController {
    private final PaymentService payments;
    public PaymentController(PaymentService payments) { this.payments=payments; }
    @PostMapping("/api/payments/checkout-session")
    public SessionResponse checkout(@AuthenticationPrincipal Users user,@Valid @RequestBody CheckoutRequest request) {
        return payments.checkout(user.getId(),request.orderId());
    }
    @PostMapping("/api/webhooks/payments")
    public WebhookResponse webhook(@RequestBody byte[] rawBody,@RequestHeader HttpHeaders headers) {
        return payments.webhook(rawBody,headers);
    }
}
