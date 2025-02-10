package com.diplom.CloudStorageService.controller;

import com.diplom.controller.AuthController;
import com.diplom.controller.UserController;
import com.diplom.request.AuthenticationRequest;
import com.diplom.request.AuthenticationResponse;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@SpringBootTest
@Testcontainers
@ExtendWith(SpringExtension.class)
public class AuthControllerIntegrationTest {

    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName("cloud_storage")
            .withUsername("testuser")
            .withPassword("testpassword");

    private final UserController userController;
    private final AuthController authController;

    @BeforeAll
    static void setUpDB() {
        postgresContainer.start();
        System.setProperty("spring.datasource.url", postgresContainer.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgresContainer.getUsername());
        System.setProperty("spring.datasource.password", postgresContainer.getPassword());
    }

    @AfterAll
    static void tearDownDB() {
        postgresContainer.stop();
    }

    @Test
    @DisplayName("Should successfully authenticate user and return JWT token")
    void shouldAuthenticateUserSuccessfully() {
        // Создаем запрос для аутентификации для имеющегося пользователя в базе
        AuthenticationRequest request = new AuthenticationRequest();
        request.setLogin("admin@admin.ru");
        request.setPassword("admin12345");

        // Выполняем запрос аутентификации
        ResponseEntity<?> response = authController.createAuthenticationToken(request);

        // Проверяем статус ответа
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // Извлекаем токен из ответа
        String jwtToken = ((AuthenticationResponse) response.getBody()).getAuth_token();
        assertNotNull(jwtToken);
        assertFalse(jwtToken.isEmpty());

        // Проверяем, что токен соответствует заголовку ответа
        assertEquals(jwtToken, response.getHeaders().getFirst("auth-token"));

    }

    @Test
    @DisplayName("Should return error when authenticating with incorrect credentials")
    void shouldReturnErrorWhenAuthenticatingWithIncorrectCredentials() {
        // Создаем запрос для аутентификации с неверными данными
        AuthenticationRequest request = new AuthenticationRequest();
        request.setLogin("wronguser");
        request.setPassword("wrongpassword");

        // Выполняем запрос аутентификации
        ResponseEntity<?> response = authController.createAuthenticationToken(request);

        // Проверяем, что статус ответа соответствует BAD_REQUEST
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        // Проверяем, что тело ответа является экземпляром Map
        assertTrue(response.getBody() instanceof Map);
        Map<String, Object> errorResponse = (Map<String, Object>) response.getBody();

        // Проверяем, что код ошибки и сообщение соответствуют ожиданиям
        assertEquals(400, errorResponse.get("id"));
        assertEquals("Неверный логин или пароль", errorResponse.get("message"));

        // Проверяем, что тело ответа не содержит других неожиданных данных
        assertFalse(errorResponse.containsKey("auth_token"), "Response should not contain auth_token");
    }


    @Test
    @DisplayName("Should successfully log out user")
    void shouldLogoutUserSuccessfully() {
        String authToken = "mocked-auth-token";

        // Выполняем запрос на выход из системы
        ResponseEntity<?> response = authController.logout(authToken);

        // Проверяем, что статус ответа соответствует OK
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Проверяем, что тело ответа содержит ожидаемое сообщение
        assertEquals("Success logout", response.getBody());

    }

}

