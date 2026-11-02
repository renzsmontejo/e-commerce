
package com.example.ecommerce.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import com.example.ecommerce.user.model.Users;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey signingKey;

    public JwtService(
            @Value("${app.jwt.secret}") String secret
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Users user) {

        Instant now = Instant.now();

        return Jwts.builder()
                .subject(user.getId().toString())
                .id(java.util.UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(
                        Date.from(now.plus(15, ChronoUnit.MINUTES))
                )
                .signWith(signingKey)
                .compact();
    }

    public Long validateAndGetUserId(String token) {
        var claims = Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token).getPayload();
        if (claims.getExpiration() == null) {
            throw new IllegalArgumentException("Token expiration is required");
        }
        return Long.valueOf(claims.getSubject());
    }
}
