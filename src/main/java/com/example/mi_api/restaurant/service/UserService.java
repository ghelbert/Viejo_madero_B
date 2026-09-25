package com.example.mi_api.restaurant.service;

import com.example.mi_api.restaurant.dto.user.UpdateUserRequest;
import com.example.mi_api.restaurant.dto.user.UserRequest;
import com.example.mi_api.restaurant.repository.UserRepository;
import java.util.List;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Map<String, Object>> findAll() {
        return userRepository.findAll();
    }

    public Map<String, Object> create(UserRequest request) {
        var passwordHash = passwordEncoder.encode(request.password());
        var id = userRepository.create(request, passwordHash);
        return Map.of("id", id, "username", request.username());
    }

    public void update(long id, UpdateUserRequest request) {
        userRepository.update(id, request);

        if (request.password() != null && !request.password().isBlank()) {
            userRepository.updatePassword(id, passwordEncoder.encode(request.password()));
        }
    }

    public void setActive(long id, boolean active) {
        userRepository.setActive(id, active);
    }
}
