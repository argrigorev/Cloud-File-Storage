package ru.netology.dimploma_project.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.netology.dimploma_project.model.Token;
import ru.netology.dimploma_project.model.User;
import ru.netology.dimploma_project.repository.TokenRepository;
import ru.netology.dimploma_project.repository.UserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AuthServiceTest {
    private TokenRepository tokenRepository;
    private UserRepository userRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        tokenRepository = mock(TokenRepository.class);
        userRepository = mock(UserRepository.class);
        authService = new AuthService(tokenRepository, userRepository);
    }

    @Test
    void login_success_createsToken() {
        User user = new User();
        user.setId(1L);
        user.setUsername("artem");
        user.setPassword("12345");

        when(userRepository.findByUsername("artem")).thenReturn(Optional.of(user));

        when(tokenRepository.save(any(Token.class))).thenAnswer(invocationOnMock -> {
            Token t = invocationOnMock.getArgument(0);
            t.setId(10L);
            return t;
        });

        Token token = authService.login("artem", "12345");

        assertNotNull(token, "Token should not be null");
        assertNotNull(token.getToken(), "Token string should not be null");
        assertFalse(token.getToken().isBlank(), "Token string should not be blank");
        assertEquals(user, token.getUser(), "Token.user should be the found user");
        assertTrue(token.getExpiresAt().isAfter(Instant.now()), "Token should expire in the future");

        verify(tokenRepository, times(1)).save(any(Token.class));
    }

    @Test
    void login_wrongUsername_throws() {
        when(userRepository.findByUsername("wrongUsername")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("wrongUsername", "pwd"));
    }

    @Test
    void login_wrongPassword_throws() {
        User user = new User();
        user.setUsername("artem");
        user.setPassword("rightPassword");

        when(userRepository.findByUsername("artem")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> authService.login("artem", "wrongPassword"));
    }

    @Test
    void logout_setsTokenRevoked() {
        Token token = new Token();
        token.setId(5L);
        token.setToken("token1");
        token.setRevoked(false);

        when(tokenRepository.findByToken("token1")).thenReturn(Optional.of(token));
        when(tokenRepository.save(any())).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

        authService.logout("token1");

        assertTrue(token.isRevoked());
        verify(tokenRepository).save(token);
    }

    @Test
    void findUserByToken_filtersRevokedAndExpired() {
        User user = new User();
        user.setUsername("artem");

        Token valid = new Token();
        valid.setToken("valid");
        valid.setUser(user);
        valid.setRevoked(false);
        valid.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));

        Token revoked = new Token();
        revoked.setToken("revoked");
        revoked.setUser(user);
        revoked.setRevoked(true);
        revoked.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));

        Token expired = new Token();
        expired.setToken("expired");
        expired.setUser(user);
        expired.setRevoked(false);
        expired.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));

        when(tokenRepository.findByToken("valid")).thenReturn(Optional.of(valid));
        when(tokenRepository.findByToken("revoked")).thenReturn(Optional.of(revoked));
        when(tokenRepository.findByToken("expired")).thenReturn(Optional.of(expired));

        assertTrue(authService.findUserByToken("valid").isPresent());
        assertFalse(authService.findUserByToken("revoked").isPresent());
        assertFalse(authService.findUserByToken("expired").isPresent());
    }
}
