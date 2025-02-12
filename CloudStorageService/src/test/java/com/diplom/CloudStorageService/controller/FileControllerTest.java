package com.diplom.CloudStorageService.controller;

import com.diplom.controller.FileController;
import com.diplom.exception.InvalidInputException;
import com.diplom.model.File;
import com.diplom.model.User;
import com.diplom.model.dto.FileDTO;
import com.diplom.repository.UserRepository;
import com.diplom.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FileControllerTest {

    private User user;

    @Mock
    private FileService fileService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FileController fileController;

    private String authToken;
    private String username;

    @BeforeEach
    void setUp() {
        authToken = "mocked-auth-token";
        username = "testuser";

        user  = new User(); // Создаем объект User
        user.setUsername("testuser");
        user.setPassword("password123");
        user.setEmail("testuser@example.com");

        // Настройка мока для UserRepository
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // Устанавливаем пользователя в SecurityContext
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
    }

    @Test
    @DisplayName("Should upload file successfully with correct parameters")
    void shouldUploadFileSuccessfully() {
        // Подготовка данных для теста
        String filename = "testfile.txt";
        MultipartFile file = new MockMultipartFile("file", filename, "text/plain", "Some content".getBytes());

        // Настройка мока для fileService
        Mockito.doNothing().when(fileService).uploadFile(any(User.class), eq(file));

        // Выполнение запроса на загрузку файла
        ResponseEntity<Void> response = fileController.uploadFile(authToken, file);

        // Проверка статуса ответа
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Проверка, что метод uploadFile был вызван один раз с правильными параметрами
        Mockito.verify(fileService, Mockito.times(1)).uploadFile(user, file);
    }



    @Test
    @DisplayName("Should list files successfully with valid parameters")
    void shouldListFilesSuccessfully() {
        // Задаем лимит для теста
        int limit = 10;

        // Создаем список с замоканными файлами
        List<File> mockedFiles = Arrays.asList(
                new File("file1.txt", 1024, LocalDateTime.parse("2023-10-01T12:00:00")),
                new File("file2.txt", 2048, LocalDateTime.parse("2023-10-01T12:00:00"))
        );

        // Настраиваем мок для fileService
        when(fileService.listFiles(any(User.class), eq(limit))).thenReturn(mockedFiles);

        // Вызываем метод контроллера
        ResponseEntity<List<FileDTO>> response = fileController.listFiles(authToken, limit);

        // Проверяем результаты
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("file1.txt", response.getBody().get(0).getFilename());
        assertEquals("file2.txt", response.getBody().get(1).getFilename());

        // Проверяем, что метод listFiles был вызван один раз
        Mockito.verify(fileService, Mockito.times(1)).listFiles(any(User.class), eq(limit));
    }


    @Test
    @DisplayName("Should download file successfully with valid parameters")
    void shouldDownloadFileSuccessfully() {
        String filename = "file1.txt";
        byte[] fileContent = "File content".getBytes();

        when(fileService.downloadFile(user, filename)).thenReturn(fileContent);

        ResponseEntity<byte[]> response = fileController.downloadFile(authToken, filename);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertArrayEquals(fileContent, response.getBody());

        // Получаем фактический заголовок Content-Disposition
        String contentDisposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        // Проверяем, что заголовок содержит ожидаемое имя файла
        assertTrue(contentDisposition.contains("file1.txt"));


        Mockito.verify(fileService, Mockito.times(1)).downloadFile(user, filename);
    }

    @Test
    @DisplayName("Should throw InvalidInputException when filename is empty during download")
    void shouldThrowInvalidInputExceptionWhenFilenameIsEmptyDuringDownload() {
        String filename = "";
        // Настройка мока для fileService, чтобы выбросить исключение
        when(fileService.downloadFile(user, filename)).thenThrow(new InvalidInputException("Filename cannot be empty."));
        // Проверка, что при вызове downloadFile выбрасывается ожидаемое исключение
        InvalidInputException exception = assertThrows(InvalidInputException.class, () -> {
            fileController.downloadFile(authToken, filename);
        });

        assertEquals("Filename cannot be empty.", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw InvalidInputException when filename is empty during upload")
    void shouldThrowInvalidInputExceptionWhenFilenameIsEmptyDuringUpload() {
        // Подготовка данных для теста
        String filename = "";
        MultipartFile file = new MockMultipartFile("file", filename, "text/plain", "Some content".getBytes());

        // Настраиваем поведение сервиса, чтобы выбрасывать исключение при пустом имени файла
        Mockito.doThrow(new InvalidInputException("Filename cannot be empty."))
                .when(fileService).uploadFile(user, file);

        // Проверяем, что при вызове контроллера выбрасывается исключение
        InvalidInputException exception = assertThrows(InvalidInputException.class, () -> {
            fileController.uploadFile(authToken, file); // Вызываем метод контроллера
        });

        // Проверяем, что сообщение исключения соответствует ожидаемому
        assertEquals("Filename cannot be empty.", exception.getMessage());
    }

}