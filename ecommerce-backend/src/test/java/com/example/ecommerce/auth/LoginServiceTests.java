package com.example.ecommerce.auth;

import com.example.ecommerce.security.JwtService;

import com.example.ecommerce.auth.service.AuthService;
import com.example.ecommerce.auth.dto.LoginRequest;
import com.example.ecommerce.user.model.Users;
import com.example.ecommerce.user.repository.UsersRepository;
import com.example.ecommerce.user.repository.RolesRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginServiceTests {
    @Test void rejectsDisabledUsersEvenWithTheCorrectPassword() {
        var users = mock(UsersRepository.class);
        var encoder = mock(PasswordEncoder.class);
        var jwt = mock(JwtService.class);
        var user = new Users(); user.setStatus("DISABLED"); user.setPasswordHash("hash");
        when(users.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("password", "hash")).thenReturn(true);
        var service = new AuthService(users, encoder, jwt, mock(RolesRepository.class), mock(com.example.ecommerce.auth.service.TokenService.class));
        var exception = assertThrows(ResponseStatusException.class,
                () -> service.login(new LoginRequest(" ADMIN@EXAMPLE.COM ", "password")));
        assertEquals(401, exception.getStatusCode().value());
        verifyNoInteractions(jwt);
    }
}
