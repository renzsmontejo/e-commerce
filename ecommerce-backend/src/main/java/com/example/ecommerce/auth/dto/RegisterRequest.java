
package com.example.ecommerce.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import java.nio.charset.StandardCharsets;

public record RegisterRequest(

        @NotBlank(message = "First name is required")
        @Size(max = 150)
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 150)
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 255)
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72)
        String password,

        @Size(max = 50)
        String phone

) {
    @AssertTrue(message = "Password must contain no more than 72 UTF-8 bytes")
    public boolean isPasswordWithinBcryptLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
