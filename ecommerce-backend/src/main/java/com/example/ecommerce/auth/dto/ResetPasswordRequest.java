package com.example.ecommerce.auth.dto;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
public record ResetPasswordRequest(@NotBlank @Size(max=255) String token,
        @NotBlank @Size(min=8,max=72) String password) {
    @AssertTrue(message="Password must contain no more than 72 UTF-8 bytes")
    public boolean isPasswordWithinBcryptLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
