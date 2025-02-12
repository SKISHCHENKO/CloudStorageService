package com.diplom.controller;

import com.diplom.jwt.JwtUtil;
import com.diplom.model.User;
import com.diplom.repository.UserRepository;
import com.diplom.request.AuthenticationRequest;
import com.diplom.request.AuthenticationResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@AllArgsConstructor
@RequestMapping("/")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    /**
     * Авторизация по почте и паролю
     */
    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthenticationRequest authenticationRequest) {
        Optional<User> optionalUser = userRepository.findByEmail(authenticationRequest.getLogin());
        if (optionalUser.isEmpty()) {
            return createErrorResponse(authenticationRequest);
        }
        User user = optionalUser.get();
        try {
            // Аутентификация пользователя
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authenticationRequest.getLogin(),
                            authenticationRequest.getPassword()
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Генерация JWT-токена
            String token = jwtUtil.generateToken(authenticationRequest.getLogin(), user.getRole());
            return ResponseEntity.ok()
                    .header("auth-token", token)
                    .body(new AuthenticationResponse(token));
        } catch (AuthenticationException e) {
            return createErrorResponse(authenticationRequest);
        }

    }

    /**
     * Сброс авторизации и выход
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "auth-token", required = true) String authToken) {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("Success logout");
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(AuthenticationRequest authenticationRequest) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("id", 400);
        errorResponse.put("message", "Неверный логин или пароль");
        errorResponse.put("email", authenticationRequest.getLogin());
        errorResponse.put("password", authenticationRequest.getPassword());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }
}