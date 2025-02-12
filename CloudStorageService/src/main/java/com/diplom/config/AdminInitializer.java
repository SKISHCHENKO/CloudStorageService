package com.diplom.config;

import com.diplom.model.Role;
import com.diplom.model.User;
import com.diplom.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdminInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initAdmin() {
        return args -> {
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin12345"))  // Хешируем пароль
                        .email("admin@admin.ru")
                        .role(Role.ROLE_ADMIN)
                        .build();
                userRepository.save(admin);
                log.info("Admin user created!");
            } else {
                log.info("Admin user already exists!");
            }
        };
    }
}