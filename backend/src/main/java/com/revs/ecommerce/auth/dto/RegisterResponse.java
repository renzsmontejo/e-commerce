
package com.revs.ecommerce.auth.dto;

public record RegisterResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String status,
        String message
) {}
