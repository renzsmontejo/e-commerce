
package com.revs.ecommerce.auth.controller;

import com.revs.ecommerce.auth.dto.RegisterRequest;
import com.revs.ecommerce.auth.dto.RegisterResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import com.revs.ecommerce.auth.dto.LoginRequest;
import com.revs.ecommerce.auth.dto.LoginResponse;
import com.revs.ecommerce.auth.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public com.revs.ecommerce.auth.dto.CurrentUserResponse currentUser(
            org.springframework.security.core.Authentication authentication) {
        var user = (com.revs.ecommerce.models.Users) authentication.getPrincipal();
        var roles = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority().substring("ROLE_".length()))
                .sorted().toList();
        return new com.revs.ecommerce.auth.dto.CurrentUserResponse(user.getId(), user.getEmail(), roles);
    }


    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        RegisterResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
