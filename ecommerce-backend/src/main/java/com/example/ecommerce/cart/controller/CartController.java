package com.example.ecommerce.cart.controller;

import com.example.ecommerce.cart.dto.CartDtos.*;
import com.example.ecommerce.cart.service.CartService;
import com.example.ecommerce.user.model.Users;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@Validated
public class CartController {
    private final CartService carts;
    public CartController(CartService carts) { this.carts=carts; }
    @GetMapping
    public CartResponse get(@AuthenticationPrincipal Users user) { return carts.get(user.getId()); }
    @PostMapping("/items")
    public ResponseEntity<CartResponse> add(@AuthenticationPrincipal Users user, @Valid @RequestBody AddItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(carts.add(user.getId(),request));
    }
    @PatchMapping("/items/{id}")
    public CartResponse change(@AuthenticationPrincipal Users user, @PathVariable @Positive Long id, @Valid @RequestBody QuantityRequest request) {
        return carts.change(user.getId(),id,request.quantity());
    }
    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal Users user, @PathVariable @Positive Long id) {
        carts.remove(user.getId(),id); return ResponseEntity.noContent().build();
    }
}
