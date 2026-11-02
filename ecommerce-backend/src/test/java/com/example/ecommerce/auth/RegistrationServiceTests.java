package com.example.ecommerce.auth;

import com.example.ecommerce.security.JwtService;

import com.example.ecommerce.auth.dto.RegisterRequest;
import com.example.ecommerce.auth.service.AuthService;
import com.example.ecommerce.user.model.Roles;
import com.example.ecommerce.user.model.Users;
import com.example.ecommerce.user.repository.UsersRepository;
import com.example.ecommerce.user.repository.RolesRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Validation;
import java.sql.SQLException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegistrationServiceTests {
    private final UsersRepository users = mock(UsersRepository.class);
    private final RolesRepository roles = mock(RolesRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final AuthService service = new AuthService(users, passwords, mock(JwtService.class), roles, mock(com.example.ecommerce.auth.service.TokenService.class));
    private RegisterRequest request() {
        return new RegisterRequest(" Test ", " Shopper ", "SHOPPER@example.com", " password123 ", null);
    }
    private Roles customerRole() {
        var customer = new Roles(); customer.setId(1L); customer.setName("CUSTOMER");
        when(roles.findByName("CUSTOMER")).thenReturn(Optional.of(customer));
        return customer;
    }
    @Test void savesOnlyCustomerRoleAndAnEncodedPassword() {
        var customer = customerRole();
        when(passwords.encode(" password123 ")).thenReturn("encoded-hash");
        when(users.saveAndFlush(any())).thenAnswer(invocation -> { Users user = invocation.getArgument(0); user.setId(2L); return user; });
        var response = service.register(request());
        var captor = org.mockito.ArgumentCaptor.forClass(Users.class);
        verify(users).saveAndFlush(captor.capture());
        var user = captor.getValue();
        assertEquals("Test", user.getFirstName());
        assertEquals("shopper@example.com", user.getEmail());
        assertEquals("encoded-hash", user.getPasswordHash());
        assertEquals(java.util.Set.of(customer), user.getRoles());
        assertEquals("ACTIVE", response.status());
        verify(roles, never()).findByName("ADMIN");
    }
    @Test void rejectsExistingEmailWithoutSaving() {
        when(users.existsByEmailIgnoreCase("shopper@example.com")).thenReturn(true);
        var exception = assertThrows(ResponseStatusException.class, () -> service.register(request()));
        assertEquals(409, exception.getStatusCode().value());
        verify(users, never()).saveAndFlush(any()); verifyNoInteractions(passwords);
    }
    @Test void translatesConcurrentDuplicateEmailToConflict() {
        customerRole(); when(passwords.encode(anyString())).thenReturn("hash");
        when(users.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate", new SQLException("duplicate", "23000", 1062)));
        var exception = assertThrows(ResponseStatusException.class, () -> service.register(request()));
        assertEquals(409, exception.getStatusCode().value());
    }
    @Test void validatesMultibytePasswordLimitBeforeEncoding() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var oversized = new RegisterRequest("Test","Shopper","test@example.com","é".repeat(37),null);
            assertFalse(validator.validate(oversized).isEmpty());
            var boundary = new RegisterRequest("Test","Shopper","test@example.com","é".repeat(36),null);
            assertTrue(validator.validate(boundary).isEmpty());
        }
    }
    @Test void refusesToRegisterIfCustomerRoleIsMissing() {
        when(roles.findByName("CUSTOMER")).thenReturn(Optional.empty());
        var exception = assertThrows(ResponseStatusException.class, () -> service.register(request()));
        assertEquals(500, exception.getStatusCode().value());
        verify(users, never()).saveAndFlush(any());
    }
}
