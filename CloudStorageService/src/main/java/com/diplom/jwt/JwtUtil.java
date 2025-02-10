package com.diplom.jwt;

import com.diplom.model.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.*;


@Component
public class JwtUtil {

    private final SecretKey secretKey;

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    // Чтение секретного ключа из конфигурации
    public JwtUtil(@Value("${jwt.secretKey}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
    }
    public String generateToken(String email, Role role) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role.name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))  // Токен действителен 1 час
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }
    // Получение имени пользователя из токена
    public String getEmail(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();  // Получаем email
        } catch (JwtException e) {
            System.out.println("Ошибка при извлечении email из токена");
            throw new IllegalArgumentException("Invalid token structure", e);
        }
    }

    // Получение роли из токена
    public Role getRole(String token) {
        String roleName = Jwts.parser()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
        return Role.valueOf(roleName);  // Преобразуем строку обратно в enum
    }

    // Проверка валидности токена
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(secretKey)  // Устанавливаем ключ для проверки подписи
                    .build()
                    .parseClaimsJws(token);
            System.out.println("✅ Token is valid!");
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // В случае ошибки токен недействителен
            System.out.println("❌ Token validation failed: " + e.getMessage());
            return false;
        }
    }

    public String resolveToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7); // Убираем префикс "Bearer "
        }
        return null;
    }
}