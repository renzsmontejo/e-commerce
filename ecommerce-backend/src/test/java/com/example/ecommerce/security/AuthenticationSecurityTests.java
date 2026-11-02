package com.example.ecommerce.security;

import com.example.ecommerce.product.storage.ProductImageStorage;

import com.example.ecommerce.auth.controller.AuthController;
import com.example.ecommerce.auth.service.AuthService;
import com.example.ecommerce.config.SecurityConfig;
import com.example.ecommerce.user.model.Users;
import com.example.ecommerce.user.model.Roles;
import com.example.ecommerce.user.repository.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Optional;
import java.util.Set;
import java.util.Date;
import java.util.Base64;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AuthController.class, AuthenticationSecurityTests.ProbeController.class})
@Import({com.example.ecommerce.product.storage.ProductImageStorage.class, SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, AuthenticationSecurityTests.ProbeController.class})
@TestPropertySource(properties = "app.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
class AuthenticationSecurityTests {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockitoBean UsersRepository users;
    @MockitoBean AuthService authService;
    @MockitoBean com.example.ecommerce.auth.service.TokenService tokens;
    private Users user;

    @BeforeEach void setup() {
        when(tokens.isActive(anyString())).thenReturn(true);
        user = new Users(); user.setId(1L); user.setEmail("admin@example.com"); user.setStatus("ACTIVE");
        var role = new Roles(); role.setName("ADMIN"); user.setRoles(Set.of(role));
        when(users.findWithRolesById(1L)).thenReturn(Optional.of(user));
    }
    @Test void rejectsMissingMalformedAndTamperedTokens() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer invalid")).andExpect(status().isUnauthorized());
        String token = jwt.generateToken(user);
        String[] parts = token.split("\\.");
        parts[2] = (parts[2].startsWith("A") ? "B" : "A") + parts[2].substring(1);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + String.join(".", parts))).andExpect(status().isUnauthorized());
    }
    @Test void returnsOnlyAuthenticatedIdentityAndRoles() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + jwt.generateToken(user)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("admin@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }
    @Test void enforcesAdminRoleOnServer() throws Exception {
        String token = jwt.generateToken(user);
        mvc.perform(get("/api/admin/probe").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        var customer = new Roles(); customer.setName("CUSTOMER"); user.setRoles(Set.of(customer));
        mvc.perform(get("/api/admin/probe").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }
    @Test void rejectsExpiredDisabledAndDeletedUsers() throws Exception {
        String token = Jwts.builder().subject("1").expiration(new Date(System.currentTimeMillis()-1000))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="))).compact();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        token = jwt.generateToken(user);
        user.setStatus("DISABLED");
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        when(users.findWithRolesById(1L)).thenReturn(Optional.empty());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test void rejectsMissingExpirationAndInvalidSubject() throws Exception {
        var key = Keys.hmacShaKeyFor(Base64.getDecoder().decode("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="));
        String noExpiry = Jwts.builder().subject("1").signWith(key).compact();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + noExpiry)).andExpect(status().isUnauthorized());
        String invalidSubject = Jwts.builder().subject("invalid-id")
                .expiration(new Date(System.currentTimeMillis() + 60_000)).signWith(key).compact();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + invalidSubject)).andExpect(status().isUnauthorized());
    }

    @Test void takesRolesFromDatabaseRatherThanTokenClaims() throws Exception {
        var customer = new Roles(); customer.setName("CUSTOMER"); user.setRoles(Set.of(customer));
        var key = Keys.hmacShaKeyFor(Base64.getDecoder().decode("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="));
        String token = Jwts.builder().subject("1").claim("roles", java.util.List.of("ADMIN"))
                .expiration(new Date(System.currentTimeMillis() + 60_000)).signWith(key).compact();
        mvc.perform(get("/api/admin/probe").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.roles[0]").value("CUSTOMER"));
    }
    @Test void exposesResetEndpointsWithoutAuthenticationAndValidatesInput() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/forgot-password")
                .contentType("application/json").content("{\"email\":\"test@example.com\"}"))
                .andExpect(status().isOk());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/reset-password")
                .contentType("application/json").content("{\"token\":\"secret\",\"password\":\"newpassword\"}"))
                .andExpect(status().isOk());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/reset-password")
                .contentType("application/json").content("{\"token\":\"secret\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }
    @Test void logoutRequiresAuthenticationAndRevokesSuppliedToken() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());
        String token=jwt.generateToken(user);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/logout")
                .header("Authorization","Bearer " + token)).andExpect(status().isNoContent());
        verify(tokens).logout(token);
        when(tokens.isActive(token)).thenReturn(false);
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer " + token)).andExpect(status().isUnauthorized());
    }
    @RestController
    static class ProbeController {
        @GetMapping("/api/admin/probe") String probe() { return "ok"; }
    }
}
