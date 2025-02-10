package com.diplom.service;

import com.diplom.exception.UserNotFoundException;
import com.diplom.model.User;
import com.diplom.model.dto.UserDTO;
import com.diplom.repository.UserRepository;
import com.diplom.request.UserRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.diplom.model.Role;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    private UserDTO convertToUserDto(User user) {
        return new UserDTO(user.getUsername(), user.getEmail());
    }

    public User save(User user) {
        return repository.save(user);
    }

    public UserDTO getUser(Long id) {
        return convertToUserDto(findById(id));
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
    public User updateUserProfile(Long id, UserRequest userRequest) {
        User user = findById(id);
        user.setUsername(userRequest.getUsername());
        user.setPassword(userRequest.getPassword());
        user.setEmail(userRequest.getEmail());
        return repository.save(user);
    }

    @Transactional
    public User createUser(UserRequest request) {
        if (repository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Пользователь уже существует");
        }
        if (repository.findByEmail(request.getEmail()).isPresent()) {
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
            throw new IllegalArgumentException("Пользователь не найден");
        }
    }
}
