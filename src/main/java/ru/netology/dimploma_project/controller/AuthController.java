package ru.netology.dimploma_project.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import ru.netology.dimploma_project.dto.LoginRequest;
import ru.netology.dimploma_project.model.Token;
import ru.netology.dimploma_project.service.AuthService;

import java.util.Map;

@RestController
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        if (loginRequest == null
                || loginRequest.getLogin() == null || loginRequest.getPassword() == null
                || loginRequest.getLogin().isBlank() || loginRequest.getPassword().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "login and password required", "id", 400));
        }

        try {
            Token token = authService.login(loginRequest.getLogin(), loginRequest.getPassword());
            Map<String, String> res = Map.of("auth-token", token.getToken());
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage(), "id", 400));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Internal server error", "id", 500));
        }
    }

    @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> logout(@RequestHeader(name = "auth-token", required = false) String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Missing auth-token header", "id", 401));
        }
        authService.logout(tokenValue);
        return ResponseEntity.ok().build();
    }
}
