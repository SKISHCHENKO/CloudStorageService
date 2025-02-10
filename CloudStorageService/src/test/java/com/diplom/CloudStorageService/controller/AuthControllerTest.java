package com.diplom.CloudStorageService.controller;

import com.diplom.controller.AuthController;
import com.diplom.jwt.JwtUtil;
import com.diplom.model.Role;
import com.diplom.model.User;
import com.diplom.repository.UserRepository;
import com.diplom.request.AuthenticationRequest;
import com.diplom.request.AuthenticationResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private AuthController authController;

    @Test
    @DisplayName("Should authenticate successfully with correct credentials")
    void shouldAuthenticateSuccessfully() {
        String username = "testuser";
        String password = "password123";
        String expectedJwt = "mocked-jwt-token";

        AuthenticationRequest request = new AuthenticationRequest(username, password);

        User mockUser = new User();
        mockUser.setEmail(username);
        mockUser.setRole(Role.ROLE_USER);

        // Мокируем поиск пользователя по email
        when(userRepository.findByEmail(username)).thenReturn(java.util.Optional.of(mockUser));

        // Мокируем аутентификацию
        Authentication mockAuth = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(mockAuth);

        // Мокируем генерацию JWT
        when(jwtUtil.generateToken(username, Role.ROLE_USER)).thenReturn(expectedJwt);

        // Выполняем запрос
        ResponseEntity<?> response = authController.createAuthenticationToken(request);

        // Проверяем, что ответ верен
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedJwt, response.getHeaders().getFirst("auth-token"));
        assertInstanceOf(AuthenticationResponse.class, response.getBody());
        assertEquals(expectedJwt, ((AuthenticationResponse) response.getBody()).getAuth_token());
    }

    @Test
    @DisplayName("Should return error when authentication fails with incorrect credentials")
    void shouldReturnErrorWhenAuthenticationFails() {
        String username = "testuser";
        String password = "wrongpassword";

        AuthenticationRequest request = new AuthenticationRequest(username, password);

        // Мокируем поиск пользователя по email
        when(userRepository.findByEmail(username)).thenReturn(java.util.Optional.empty());

        // Выполняем запрос
        ResponseEntity<?> response = authController.createAuthenticationToken(request);

        // Проверяем, что ошибка с кодом 400
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertInstanceOf(Map.class, response.getBody());
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> errorResponse = (java.util.Map<String, Object>) response.getBody();
        assertEquals("Неверный логин или пароль", errorResponse.get("message"));
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        String username = "unknownuser";
        String password = "password123";

        AuthenticationRequest request = new AuthenticationRequest(username, password);

        // Мокируем, что пользователя не нашли в базе
        when(userRepository.findByEmail(username)).thenReturn(java.util.Optional.empty());

        // Выполняем запрос
        ResponseEntity<?> response = authController.createAuthenticationToken(request);

        // Проверяем, что возвращается ошибка 400
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertInstanceOf(Map.class, response.getBody());
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> errorResponse = (java.util.Map<String, Object>) response.getBody();
        assertEquals("Неверный логин или пароль", errorResponse.get("message"));
    }

    @Test
    @DisplayName("Should log out successfully when auth token is provided")
    void shouldLogoutSuccessfully() {
        String authToken = "mocked-jwt-token";

        ResponseEntity<?> response = authController.logout(authToken);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Success logout", response.getBody());
    }

    @Test
    @DisplayName("Should return error when authentication fails due to wrong credentials")
    void shouldThrowErrorWhenAuthenticationFails() {
        String username = "invaliduser";
        String password = "wrongpassword";

        AuthenticationRequest authenticationRequest = new AuthenticationRequest(username, password);

        // Мокируем ситуацию, когда пользователь не найден
        when(userRepository.findByEmail(username)).thenReturn(java.util.Optional.empty());

        // Выполняем запрос
        ResponseEntity<?> response = authController.createAuthenticationToken(authenticationRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}