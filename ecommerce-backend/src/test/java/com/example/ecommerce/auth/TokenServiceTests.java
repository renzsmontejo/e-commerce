package com.example.ecommerce.auth;
import com.example.ecommerce.auth.service.TokenService;
import com.example.ecommerce.auth.model.AuthTokens;
import com.example.ecommerce.user.model.Users;
import com.example.ecommerce.auth.repository.AuthTokensRepository;
import com.example.ecommerce.user.repository.UsersRepository;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class TokenServiceTests {
    final AuthTokensRepository tokens=mock(AuthTokensRepository.class);
    final UsersRepository users=mock(UsersRepository.class);
    final PasswordEncoder passwords=mock(PasswordEncoder.class);
    final JavaMailSender mail=mock(JavaMailSender.class);
    final TokenService service=new TokenService(tokens,users,passwords,mail,"http://localhost/reset","test@example.com");
    Users user() { var u=new Users(); u.setId(1L); u.setStatus("ACTIVE"); u.setEmail("test@example.com"); return u; }
    @Test void sendsResetLinkButStoresOnlyHash() {
        when(users.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user()));
        service.forgotPassword(" TEST@example.com ");
        var message=org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        var record=org.mockito.ArgumentCaptor.forClass(AuthTokens.class);
        verify(mail).send(message.capture()); verify(tokens).save(record.capture());
        String raw=message.getValue().getText().split("token=")[1];
        assertEquals(TokenService.hash(raw),record.getValue().getTokenHash());
        assertEquals("PASSWORD_RESET",record.getValue().getTokenType());
        assertTrue(record.getValue().isValid());
    }
    @Test void unknownEmailDoesNotSendOrStoreToken() {
        service.forgotPassword("missing@example.com"); verifyNoInteractions(tokens,mail);
    }
    @Test void resetHashesPasswordAndRevokesAllTokens() {
        var t=new AuthTokens(); t.setUser(user()); t.setExpiresAt(LocalDateTime.now().plusMinutes(1));
        when(tokens.findResetForUpdate(TokenService.hash("secret"))).thenReturn(Optional.of(t));
        when(passwords.encode("newpassword")).thenReturn("encoded");
        service.resetPassword("secret","newpassword");
        assertEquals("encoded",t.getUser().getPasswordHash());
        verify(users).save(t.getUser()); verify(tokens).revokeAllForUser(1L);
    }
    @Test void rejectsExpiredAndConsumedTokens() {
        var t=new AuthTokens(); t.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(tokens.findResetForUpdate(anyString())).thenReturn(Optional.of(t));
        assertThrows(ResponseStatusException.class,()->service.resetPassword("secret","newpassword"));
        t.setExpiresAt(LocalDateTime.now().plusMinutes(1)); t.revoke();
        assertThrows(ResponseStatusException.class,()->service.resetPassword("secret","newpassword"));
        verifyNoInteractions(users,passwords);
    }
    @Test void logoutMakesTokenInactive() {
        var t=new AuthTokens(); t.setExpiresAt(LocalDateTime.now().plusMinutes(1));
        when(tokens.findByTokenHashAndTokenType(TokenService.hash("jwt"),"ACCESS")).thenReturn(Optional.of(t));
        assertTrue(service.isActive("jwt")); service.logout("jwt"); assertFalse(service.isActive("jwt"));
    }
}
