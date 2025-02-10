package com.diplom.controller;

import com.diplom.model.User;
import com.diplom.request.UserRequest;
import com.diplom.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @PostMapping()
    public ResponseEntity<String> addUser(@RequestBody UserRequest userRequest) {
        try {
            User newUser = userService.createUser(userRequest);
            return ResponseEntity.ok("Новый пользователь "+ newUser.getUsername()+ " создан.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // Получение информации о текущем пользователе
    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        return ResponseEntity.ok(user);
    }

    // Редактирование профиля
    @PatchMapping("/me")
    public ResponseEntity<String> updateUserProfile(Authentication auth, @RequestBody UserRequest userRequest) {
        try {
            User user = userService.findByUsername(auth.getName());
            userService.updateUserProfile(user.getId(), userRequest);
            return ResponseEntity.ok("Профиль успешно обновлен.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при обновление профиля: " + e.getMessage());
        }
    }

    // Удаление пользователя
    @DeleteMapping("/me")
    public ResponseEntity<String> deleteUser(Authentication auth) {
        try {
            User user = userService.findByUsername(auth.getName());
            userService.deleteUser(user.getId());
            return ResponseEntity.ok("Пользователь усшпешно удален.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при удаление пользователя: " + e.getMessage());
        }
    }
}