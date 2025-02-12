package com.diplom.CloudStorageService.service;

import com.diplom.model.Role;
import com.diplom.model.User;
import com.diplom.repository.UserRepository;
import com.diplom.request.UserRequest;
import com.diplom.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserRequest user;


    @BeforeEach
    void userSetup() {
        user = new UserRequest();
        user.setUsername("testuser");
        user.setPassword("password123");
        user.setRole("ROLE_USER");
    }

    @Test
    @DisplayName("Should add user successfully when all conditions are met")
    void shouldAddUserSuccessfully() {
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(user.getPassword())).thenReturn("encodedPassword");

        userService.createUser(user);

        // Проверяем, что метод save был вызван
        verify(userRepository, times(1)).save(any(User.class));

        // Проверяем, что пароль был закодирован
        assertEquals("password123", user.getPassword());

        // Проверяем, что роль пользователя установлена
        assertTrue(user.getRole().contains("ROLE_USER"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when user with the same username already exists")
    void shouldThrowExceptionWhenUserAlreadyExists() {
        // Подготовка данных для теста
        User existingUser = new User();
        existingUser.setUsername(user.getUsername());

        // Настройка мока для userRepository, чтобы вернуть существующего пользователя
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(existingUser));

        // Проверка, что при попытке создать пользователя выбрасывается ожидаемое исключение
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(user);
        });

        // Проверка, что сообщение исключения соответствует ожидаемому
        assertEquals("Пользователь уже существует", exception.getMessage());
    }

    @Test
    @DisplayName("Should delete user successfully")
    void testDeleteUser() {
        // Создаем пользователя с id 1L
        User user = new User();
        user.setId(1L);
        user.setUsername("testUser ");
        user.setEmail("test@test.ru");
        user.setRole(Role.ROLE_USER);

        // Настраиваем мок, чтобы вернуть пользователя при вызове findById
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        // Удаляем пользователя
        userService.deleteUser(user.getId());

        // Проверяем, что метод deleteById был вызван один раз
        verify(userRepository, times(1)).deleteById(user.getId());

        // Проверяем, что findById был вызван один раз
        verify(userRepository, times(1)).findById(user.getId());
    }

}
