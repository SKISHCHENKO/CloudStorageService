package com.diplom.controller;

import com.diplom.model.User;
import com.diplom.request.UserRequest;
import com.diplom.request.UserUpdateRequest;
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
            return ResponseEntity.ok("New user created and saved");
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
    public ResponseEntity<String> updateUserProfile(Authentication auth, @RequestBody UserUpdateRequest userUpdateRequest) {
        try {
            User user = userService.findByUsername(auth.getName());
            userService.updateUserProfile(user.getId(), userUpdateRequest);
            return ResponseEntity.ok("Profile updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update profile: " + e.getMessage());
        }
    }

    // Удаление профиля
    @DeleteMapping("/me")
    public ResponseEntity<String> deleteUserProfile(Authentication auth) {
        try {
            User user = userService.findByUsername(auth.getName());
            userService.deleteUser(user.getId());
            return ResponseEntity.ok("Profile deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete profile: " + e.getMessage());
        }
    }
}