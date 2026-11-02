
package com.example.ecommerce.auth.dto;

public record RegisterResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String status,
        String message
) {}
