package com.diplom.CloudStorageService.controller;

import com.diplom.controller.FileController;
import com.diplom.controller.UserController;
import com.diplom.model.dto.FileDTO;
import com.diplom.repository.FileRepository;
import com.diplom.request.UserRequest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@SpringBootTest
@Testcontainers
@ExtendWith(SpringExtension.class)
public class FileControllerIntegrationTest {

    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName("cloud_storage")
            .withUsername("testuser")
            .withPassword("testpassword");

    private final FileController fileController;
    private final UserController userController;
    private final FileRepository fileRepository;

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

    @BeforeEach
    void setUp() {
        // Создаем мок-объект Authentication
        Authentication auth = Mockito.mock(Authentication.class);
        Mockito.when(auth.getName()).thenReturn("testuser");
        // Устанавливаем мок-объект в SecurityContext
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        // Удаляем пользователя перед каждым тестом
        try {
            userController.deleteUser (auth);
        } catch (Exception ignored) {
        }

        // Создаем нового пользователя
        UserRequest testUser  = new UserRequest();
        testUser.setUsername("testuser");
        testUser.setPassword("testpassword");
        testUser.setEmail("test@test.ru");
        testUser.setRole("ROLE_USER");

        // Проверяем, что пользователь успешно создан
        ResponseEntity<String> response = userController.addUser (testUser );
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Пользователь не был создан.");
    }

    @AfterEach
    void cleanUp() {
        // Удаляем все файлы после каждого теста
        fileController.deleteFile("mocked-auth-token", "testfile.txt");
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should list files successfully")
    void shouldListFilesSuccessfully() {
        // Сначала загружаем файл для тестирования
        MultipartFile file = Mockito.mock(MultipartFile.class);
        Mockito.when(file.getOriginalFilename()).thenReturn("testfile.txt");
        Mockito.when(file.getSize()).thenReturn(1024L);
        Mockito.when(file.isEmpty()).thenReturn(false);
        fileController.uploadFile("mocked-auth-token", file);

        // Теперь проверяем, что файлы успешно перечисляются
        ResponseEntity<List<FileDTO>> response = fileController.listFiles("mocked-auth-token", 10);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    @DisplayName("Should download file successfully")
    void shouldDownloadFileSuccessfully() throws IOException {
        // Создаем временный файл для тестирования
        File tempFile = File.createTempFile("testfile", ".txt");
        String fileContent = "Test file content";
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(fileContent);
        }

        // Создаем MultipartFile из временного файла
        MultipartFile file = new MockMultipartFile("file", tempFile.getName(), "text/plain", new FileInputStream(tempFile));

        // Загружаем файл через контроллер
        ResponseEntity<Void> uploadResponse = fileController.uploadFile("mocked-auth-token", file);
        assertEquals(HttpStatus.OK, uploadResponse.getStatusCode(), "File upload failed");

        // Теперь проверяем, что файл успешно скачивается
        ResponseEntity<byte[]> response = fileController.downloadFile("mocked-auth-token", "testfile.txt");

        // Проверяем статус ответа
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Проверяем содержимое файла
        assertEquals(fileContent, new String(response.getBody()));

        // Проверяем заголовок Content-Disposition
        assertEquals("attachment; filename=\"testfile.txt\"", response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));

        // Удаляем временный файл
        tempFile.delete();
    }

}