package com.revs.ecommerce.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Long userId,
        String email
) {}