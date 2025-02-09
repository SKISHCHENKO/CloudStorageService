package com.diplom.service;

import com.diplom.exception.UserNotFoundException;
import com.diplom.model.User;
import com.diplom.model.dto.UserDTO;
import com.diplom.repository.UserRepository;
import com.diplom.request.UserRequest;
import com.diplom.request.UserUpdateRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.diplom.model.Role;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final FileService fileService;
    private final ModelMapper mapper = new ModelMapper(); // Инициализирован ModelMapper
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private UserDTO convertToUserDto(User user) {
        return mapper.map(user, UserDTO.class);
    }

    public User save(User user) {
        return repository.save(user);
    }

    public UserDTO getUser(Long id) {
        return convertToUserDto(findById(id));
    }

    public User findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    public User findByUsername(String name) {
        return repository.findByUsername(name)
                .orElseThrow(() -> new UserNotFoundException("User not found with name: " + name));
    }

    public User findByEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }

    @Transactional
    public User updateUserProfile(Long id, UserUpdateRequest userUpdateRequest) {
        User user = findById(id);
        mapper.map(userUpdateRequest, user);  // обновление данных пользователя
        logger.info("User {} is updated", id);
        return repository.save(user);
    }

    @Transactional
    public User createUser(UserRequest request) {
        if (repository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("User with that username already exists");
        }
        if (repository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User with that email already exists");
        }

        User newUser = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .username(request.getUsername())
                .role(Role.ROLE_USER)
                .build();

        logger.info("New user is created: {}", newUser.getUsername());
        return repository.save(newUser);
    }

    @Transactional
    public void addFileToUsersUploadedFiles(Long fileAuthorId, Long id) {
        User user = findById(fileAuthorId);
        user.addFileToUsersUploadedFiles(id);
        repository.save(user);
        logger.info("User {} uploaded file {}", fileAuthorId, id);
    }

    @Transactional
    public void deleteUser(Long userId) {
        repository.deleteById(userId);
        logger.info("User {} is deleted", userId);
    }
}
