
package com.revs.ecommerce.auth.service;

import java.util.Locale;

import com.revs.ecommerce.auth.dto.RegisterResponse;
import com.revs.ecommerce.repositories.RolesRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.revs.ecommerce.auth.dto.LoginRequest;
import com.revs.ecommerce.auth.dto.LoginResponse;
import com.revs.ecommerce.auth.dto.RegisterRequest;
import com.revs.ecommerce.auth.dto.RegisterResponse;
import com.revs.ecommerce.models.Roles;
import com.revs.ecommerce.models.Users;
import com.revs.ecommerce.repositories.UsersRepository;
import com.revs.ecommerce.security.JwtService;

@Service
public class AuthService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RolesRepository rolesRepository;

    public AuthService(
            UsersRepository usersRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RolesRepository rolesRepository
    ) {
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rolesRepository = rolesRepository;
    }

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

        if (!passwordMatches) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        // TODO: Check your existing user status field
        // and reject disabled or locked accounts.

        String token = jwtService.generateToken(user);

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

        Users savedUser = usersRepository.save(user);

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
