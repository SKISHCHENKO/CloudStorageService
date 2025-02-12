package com.diplom.service;

import com.diplom.exception.UserNotFoundException;
import com.diplom.model.User;
import com.diplom.repository.UserRepository;
import com.diplom.request.UserRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.diplom.model.Role;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;



    public User save(User user) {
        return repository.save(user);
    }

    public User findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с id : " + id + " не найден."));
    }

    public User findByUsername(String name) {
        return repository.findByUsername(name)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с именем" + name + " не найден."));
    }

    public User findByEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с email: " + email + " не найден"));
    }

    @Transactional
    public User updateUserProfile(User user, UserRequest userRequest) {
        user.setUsername(userRequest.getUsername());
        user.setPassword(userRequest.getPassword());
        user.setEmail(userRequest.getEmail());
        return repository.save(user);
    }

    @Transactional
    public User createUser(UserRequest request) {
        if (repository.findByUsername(request.getUsername()).isPresent()) {
            log.error("Пользователь уже существует");
            throw new IllegalArgumentException("Пользователь уже существует");
        }
        if (repository.findByEmail(request.getEmail()).isPresent()) {
            log.error("Пользователь уже существует");
            throw new IllegalArgumentException("Пользователь уже существует");
        }

        User newUser = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .username(request.getUsername())
                .role(Role.ROLE_USER)
                .build();

        return repository.save(newUser);
    }

    @Transactional
    public void deleteUser (Long userId) {
        Optional<User> user = repository.findById(userId);
        if (user.isPresent()) {
            repository.deleteById(userId);
        } else {
            log.error("Пользователь не найден");
            throw new IllegalArgumentException("Пользователь не найден");
        }
    }
}
