package com.example.mi_api.restaurant.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.mi_api.restaurant.dto.auth.LoginRequest;
import com.example.mi_api.restaurant.exception.InvalidCredentialsException;
import com.example.mi_api.restaurant.repository.AuthRepository;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthRepository authRepository, PasswordEncoder passwordEncoder) {
        this.authRepository = authRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Map<String, Object> login(LoginRequest request) {
        logger.info("Authenticating login request");
        var users = authRepository.findByUsername(request.username());

        if (users.isEmpty()
                || !passwordEncoder.matches(
                        request.password(),
                        (String) users.getFirst().get("password_hash"))) {
            logger.warn("Login failed: invalid credentials");
            throw new InvalidCredentialsException("Usuario o contraseña incorrectos");
        }

        var user = users.getFirst();
        logger.info("Login successful");
        return Map.of(
                "id", user.get("id"),
                "fullName", user.get("full_name"),
                "username", user.get("username"),
                "role", user.get("role"));
    }
}
