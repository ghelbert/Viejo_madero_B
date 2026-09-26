package com.example.mi_api.restaurant.service;

import java.util.List;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.mi_api.restaurant.dto.user.UpdateUserRequest;
import com.example.mi_api.restaurant.dto.user.UserRequest;
import com.example.mi_api.restaurant.exception.DuplicateUsernameException;
import com.example.mi_api.restaurant.repository.UserRepository;

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
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateUsernameException();
        }
        var passwordHash = passwordEncoder.encode(request.password());
        try {
            var id = userRepository.create(request, passwordHash);
            return Map.of("id", id, "username", request.username());
        } catch (DuplicateKeyException exception) {
            throw new DuplicateUsernameException();
        }
    }

    public void update(long id, UpdateUserRequest request) {
        if (userRepository.existsByUsernameForAnotherUser(request.username(), id)) {
            throw new DuplicateUsernameException();
        }
        try {
            userRepository.update(id, request);
        } catch (DuplicateKeyException exception) {
            throw new DuplicateUsernameException();
        }

        if (request.password() != null && !request.password().isBlank()) {
            userRepository.updatePassword(id, passwordEncoder.encode(request.password()));
        }
    }

    public void setActive(long id, boolean active) {
        userRepository.setActive(id, active);
    }
}
