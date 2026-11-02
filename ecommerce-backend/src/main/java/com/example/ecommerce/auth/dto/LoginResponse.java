package com.example.ecommerce.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Long userId,
        String email
) {}