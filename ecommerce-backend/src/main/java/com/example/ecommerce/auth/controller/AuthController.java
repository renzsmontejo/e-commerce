
package com.example.ecommerce.auth.controller;

import com.example.ecommerce.user.model.Users;

import com.example.ecommerce.auth.dto.RegisterRequest;
import com.example.ecommerce.auth.dto.RegisterResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import com.example.ecommerce.auth.dto.LoginRequest;
import com.example.ecommerce.auth.dto.LoginResponse;
import com.example.ecommerce.auth.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final com.example.ecommerce.auth.service.TokenService tokens;

    public AuthController(AuthService authService, com.example.ecommerce.auth.service.TokenService tokens) {
        this.authService = authService;
        this.tokens = tokens;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public com.example.ecommerce.auth.dto.CurrentUserResponse currentUser(
            org.springframework.security.core.Authentication authentication) {
        var user = (com.example.ecommerce.user.model.Users) authentication.getPrincipal();
        var roles = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority().substring("ROLE_".length()))
                .sorted().toList();
        return new com.example.ecommerce.auth.dto.CurrentUserResponse(user.getId(), user.getEmail(), roles);
    }


    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        RegisterResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    @PostMapping("/forgot-password")
    public com.example.ecommerce.auth.dto.MessageResponse forgotPassword(
            @Valid @RequestBody com.example.ecommerce.auth.dto.ForgotPasswordRequest request) {
        tokens.forgotPassword(request.email());
        return new com.example.ecommerce.auth.dto.MessageResponse("If an active account exists, a password reset link has been sent.");
    }
    @PostMapping("/reset-password")
    public com.example.ecommerce.auth.dto.MessageResponse resetPassword(
            @Valid @RequestBody com.example.ecommerce.auth.dto.ResetPasswordRequest request) {
        tokens.resetPassword(request.token(), request.password());
        return new com.example.ecommerce.auth.dto.MessageResponse("Password reset successful");
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
        tokens.logout(authorization.substring(7));
        return ResponseEntity.noContent().build();
    }
}
