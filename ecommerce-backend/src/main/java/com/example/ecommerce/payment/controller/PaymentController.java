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
    @io.swagger.v3.oas.annotations.Operation(summary="Verify a PayMongo payment notification",
        parameters=@io.swagger.v3.oas.annotations.Parameter(name="Paymongo-Signature",in=io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER,required=true),
        requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(required=true,content=@io.swagger.v3.oas.annotations.media.Content(
            mediaType="application/json",schema=@io.swagger.v3.oas.annotations.media.Schema(type="object"))))
    @PostMapping("/api/webhooks/payments")
    public WebhookResponse webhook(@RequestBody byte[] rawBody,@io.swagger.v3.oas.annotations.Parameter(hidden=true) @RequestHeader HttpHeaders headers) {
        return payments.webhook(rawBody,headers);
    }
}
