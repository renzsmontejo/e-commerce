package com.example.ecommerce.auth.service;

import com.example.ecommerce.auth.model.AuthTokens;
import com.example.ecommerce.user.model.Users;
import com.example.ecommerce.auth.repository.AuthTokensRepository;
import com.example.ecommerce.user.repository.UsersRepository;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class TokenService {
    private final AuthTokensRepository tokens;
    private final UsersRepository users;
    private final PasswordEncoder passwords;
    private final JavaMailSender mail;
    private final String resetUrl;
    private final String from;
    public TokenService(AuthTokensRepository tokens, UsersRepository users, PasswordEncoder passwords,
            JavaMailSender mail, @Value("${app.auth.reset-url}") String resetUrl,
            @Value("${app.auth.mail-from}") String from) {
        this.tokens=tokens; this.users=users; this.passwords=passwords; this.mail=mail;
        this.resetUrl=resetUrl; this.from=from;
    }
    public static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private void store(Users user, String raw, String type, LocalDateTime expiry) {
        var token=new AuthTokens(); token.setUser(user); token.setTokenHash(hash(raw));
        token.setTokenType(type); token.setExpiresAt(expiry); tokens.save(token);
    }
    public void recordLogin(Users user, String jwt) {
        store(user,jwt,"ACCESS",LocalDateTime.now().plusMinutes(15));
    }
    public boolean isActive(String jwt) {
        return tokens.findByTokenHashAndTokenType(hash(jwt),"ACCESS").filter(AuthTokens::isValid).isPresent();
    }
    @Transactional
    public void logout(String jwt) {
        tokens.findByTokenHashAndTokenType(hash(jwt),"ACCESS").ifPresent(token -> { token.revoke(); tokens.save(token); });
    }
    @Transactional
    public void forgotPassword(String email) {
        users.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT))
            .filter(user -> "ACTIVE".equals(user.getStatus())).ifPresent(user -> {
                byte[] bytes=new byte[32]; new SecureRandom().nextBytes(bytes);
                String raw=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                store(user,raw,"PASSWORD_RESET",LocalDateTime.now().plusMinutes(30));
                String url=UriComponentsBuilder.fromUriString(resetUrl).queryParam("token",raw).build().encode().toUriString();
                var message=new SimpleMailMessage(); message.setFrom(from); message.setTo(user.getEmail());
                message.setSubject("Reset your password");
                message.setText("Reset your password using this link (valid for 30 minutes): " + url);
                mail.send(message);
            });
    }
    @Transactional
    public void resetPassword(String raw, String password) {
        var token=tokens.findResetForUpdate(hash(raw)).filter(AuthTokens::isValid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid or expired reset token"));
        var user=token.getUser();
        if (!"ACTIVE".equals(user.getStatus())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid or expired reset token");
        user.setPasswordHash(passwords.encode(password)); users.save(user);
        tokens.revokeAllForUser(user.getId());
    }
}
