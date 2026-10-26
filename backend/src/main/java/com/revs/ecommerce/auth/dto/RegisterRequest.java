
package com.revs.ecommerce.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "First name is required")
        @Size(max = 150)
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 150)
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72)
        String password,

        @Size(max = 50)
        String phone

) {}
