
package com.example.ecommerce.auth.service;

import java.util.Locale;

import com.example.ecommerce.user.repository.RolesRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.ecommerce.auth.dto.LoginRequest;
import com.example.ecommerce.auth.dto.LoginResponse;
import com.example.ecommerce.auth.dto.RegisterRequest;
import com.example.ecommerce.auth.dto.RegisterResponse;
import com.example.ecommerce.user.model.Roles;
import com.example.ecommerce.user.model.Users;
import com.example.ecommerce.user.repository.UsersRepository;
import com.example.ecommerce.security.JwtService;

@Service
public class AuthService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RolesRepository rolesRepository;
    private final TokenService tokenService;

    public AuthService(
            UsersRepository usersRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RolesRepository rolesRepository, TokenService tokenService
    ) {
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rolesRepository = rolesRepository;
        this.tokenService = tokenService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        Users user = usersRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Invalid email or password"
                        )
                );

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        );

        if (!passwordMatches || !"ACTIVE".equals(user.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        String token = jwtService.generateToken(user);
        tokenService.recordLogin(user, token);

        return new LoginResponse(
                token,
                "Bearer",
                user.getId(),
                user.getEmail()
        );
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (usersRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered"
            );
        }

        Roles customerRole = rolesRepository
                .findByName("CUSTOMER")
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "CUSTOMER role is not configured"
                        )
                );

        Users user = new Users();

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);

        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );

        user.setPhone(request.phone());
        user.setStatus("ACTIVE");

        // Assign CUSTOMER role using existing ManyToMany
        user.getRoles().add(customerRole);

        // createdAt and updatedAt are handled
        // automatically by @PrePersist in Users.java.

        Users savedUser;
        try {
            savedUser = usersRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            // A concurrent registration can win after the email pre-check.
            Throwable cause = exception;
            while (cause != null) {
                if (cause instanceof java.sql.SQLException sql && sql.getErrorCode() == 1062) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
                }
                cause = cause.getCause();
            }
            throw exception;
        }

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getEmail(),
                savedUser.getStatus(),
                "Registration successful"
        );
    }
}
