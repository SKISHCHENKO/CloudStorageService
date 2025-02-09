package com.diplom.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Сущность пользователя системы.
 * Хранит основную информацию о пользователе.
 */

@Data
@Entity
@Table(name = "\"user\"")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "email", length = 50, unique = true, nullable = false)
    private String email;

    @Transient
    List<Long> uploadedFiles; // файлы пользователя - список id файлов

    public void addFileToUsersUploadedFiles(Long id) {
        if (uploadedFiles == null) {
            uploadedFiles = new ArrayList<>();
        }
        this.uploadedFiles.add(id);
    }
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Преобразуем роль в объект GrantedAuthority
        return Collections.singletonList(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;  // Возвращает true, если аккаунт не истек
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;  // Возвращает true, если аккаунт не заблокирован
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;  // Возвращает true, если учетные данные не истекли
    }

    @Override
    public boolean isEnabled() {
        return true;  // Возвращает true, если аккаунт активен
    }
}
