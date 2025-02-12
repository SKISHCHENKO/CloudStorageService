package com.diplom.CloudStorageService.controller;

import com.diplom.controller.UserController;
import com.diplom.model.User;
import com.diplom.request.UserRequest;
import com.diplom.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    @DisplayName("Should add user successfully with valid data")
    void shouldAddUserSuccessfully() {
        UserRequest user = new UserRequest();
        user.setUsername("testuser");
        user.setPassword("password123");
        user.setEmail("testuser@example.com");

        User newUser  = new User(); // Создаем объект User
        newUser.setUsername("testuser");
        newUser.setPassword("password123");
        newUser.setEmail("testuser@example.com");

        Mockito.when(userService.createUser (user)).thenReturn(newUser);

        ResponseEntity<String> response = userController.addUser (user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Новый пользователь testuser создан.", response.getBody());
        Mockito.verify(userService, Mockito.times(1)).createUser (user);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when username already exists")
    void shouldThrowExceptionWhenUsernameAlreadyExists() {
        UserRequest user = new UserRequest();
        user.setUsername("existingUser ");
        user.setPassword("password123");
        user.setEmail("existinguser@example.com");

        // Настраиваем мок для выбрасывания исключения
        Mockito.doThrow(new IllegalArgumentException("Пользователь уже существует"))
                .when(userService).createUser (user);

        ResponseEntity<String> response = userController.addUser (user);

        // Проверяем, что статус ответа - 500 (внутренняя ошибка сервера)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Пользователь уже существует", response.getBody());
    }

}
