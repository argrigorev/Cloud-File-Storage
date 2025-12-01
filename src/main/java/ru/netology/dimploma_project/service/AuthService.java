package ru.netology.dimploma_project.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import ru.netology.dimploma_project.model.Token;
import ru.netology.dimploma_project.model.User;
import ru.netology.dimploma_project.repository.TokenRepository;
import ru.netology.dimploma_project.repository.UserRepository;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

@Service
public class AuthService {
    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();

    private static final Logger logger = LogManager.getLogger(AuthService.class);

    private static final long TOKEN_TTL_DAYS = 1;

    public AuthService(TokenRepository tokenRepository, UserRepository userRepository) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    public Token login(String username, String password) {
        logger.info("Попытка входа пользователя '{}'", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.warn("Неудачный вход: неправильный username '{}'", username);
                    return new IllegalArgumentException("Wrong username.");
                });

        if (!user.getPassword().equals(password)) {
            logger.warn("Неудачный вход: неправильный пароль пользователя '{}'", username);
            throw new IllegalArgumentException("Wrong password.");
        }

        String tokenValue = generateNewToken();
        logger.info("Пользователь '{}' успешно вошёл, создан токен", username);

        Token token = new Token();
        token.setToken(tokenValue);
        token.setUser(user);
        token.setExpiresAt(Instant.now().plus(TOKEN_TTL_DAYS, ChronoUnit.DAYS));
        token.setRevoked(false);

        return tokenRepository.save(token);
    }

    private String normalizeToken(String token) {
        token = token.trim();
        if (token.toLowerCase().startsWith("bearer ")) {
            return token.substring(7).trim();
        }
        return token;
    }

    public void logout(String tokenValue) {
        logger.info("Попытка выхода по токену");

        if (tokenValue == null || tokenValue.isBlank()) {
            logger.warn("Токен отсутствует, выход невозможен");
            return;
        }

        String normalized = normalizeToken(tokenValue);
        tokenRepository.findByToken(normalized).ifPresent(t -> {
            logger.info("Установление статуса revoked для токена");
            t.setRevoked(true);
            tokenRepository.save(t);
        });
    }

    public Optional<User> findUserByToken(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) {
            logger.warn("Токен пустой");
            return Optional.empty();
        }

        String normalized = normalizeToken(tokenValue);
        return tokenRepository.findByToken(normalized)
                .filter(t -> !t.isRevoked() && t.getExpiresAt().isAfter(Instant.now()))
                .map(Token::getUser);
    }

    public static String generateNewToken() {
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }
}
