
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
        System.out.println("LOGIN ENDPOINT REACHED: " + request.email());
        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(response);
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
