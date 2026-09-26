package com.example.mi_api.restaurant.service;

import com.example.mi_api.restaurant.dto.auth.LoginRequest;
import com.example.mi_api.restaurant.exception.InvalidCredentialsException;
import com.example.mi_api.restaurant.repository.AuthRepository;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthRepository authRepository, PasswordEncoder passwordEncoder) {
        this.authRepository = authRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Map<String, Object> login(LoginRequest request) {
        var users = authRepository.findByUsername(request.username());

        if (users.isEmpty()
                || !passwordEncoder.matches(
                        request.password(),
                        (String) users.getFirst().get("password_hash"))) {
            throw new InvalidCredentialsException("Usuario o contraseña incorrectos");
        }

        var user = users.getFirst();
        return Map.of(
                "id", user.get("id"),
                "fullName", user.get("full_name"),
                "username", user.get("username"),
                "role", user.get("role"));
    }
}
