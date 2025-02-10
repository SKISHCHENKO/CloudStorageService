package com.diplom.CloudStorageService.controller;

import com.diplom.controller.FileController;
import com.diplom.exception.InvalidInputException;
import com.diplom.model.dto.FileDTO;
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

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FileControllerTest {

    @Mock
    private FileService fileService;

    @InjectMocks
    private FileController fileController;

    private String authToken;
    private String username;

    @BeforeEach
    void setUp() {
        authToken = "mocked-auth-token";
        username = "testuser";
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(username, null));
    }

    @Test
    @DisplayName("Should upload file successfully with correct parameters")
    void shouldUploadFileSuccessfully() {
        // Подготовка данных для теста
        String filename = "testfile.txt";
        MultipartFile file = new MockMultipartFile("file", filename, "text/plain", "Some content".getBytes());
        // Выполнение запроса на загрузку файла
        ResponseEntity<Void> response = fileController.uploadFile(authToken, file);
        // Проверка статуса ответа
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Проверка, что метод uploadFile был вызван один раз с правильными параметрами
        Mockito.verify(fileService, Mockito.times(1)).uploadFile(username, file);
    }


    @Test
    @DisplayName("Should list files successfully with valid parameters")
    void shouldListFilesSuccessfully() {
        int limit = 10;
        FileDTO file1 = new FileDTO("file1.txt", 1024, "2023-10-01 12:00:00");
        FileDTO file2 = new FileDTO("file2.txt", 2048, "2023-10-01 12:00:00");

        List<FileDTO> mockedFiles = Arrays.asList(file1, file2);

        when(fileService.listFiles(username, limit)).thenReturn(mockedFiles);

        ResponseEntity<List<FileDTO>> response = fileController.listFiles(authToken, limit);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("file1.txt", response.getBody().get(0).getFilename());
        Mockito.verify(fileService, Mockito.times(1)).listFiles(username, limit);
    }

    @Test
    @DisplayName("Should download file successfully with valid parameters")
    void shouldDownloadFileSuccessfully() {
        String filename = "file1.txt";
        byte[] fileContent = "File content".getBytes();

        when(fileService.downloadFile(username, filename)).thenReturn(fileContent);

        ResponseEntity<byte[]> response = fileController.downloadFile(authToken, filename);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertArrayEquals(fileContent, response.getBody());

        // Получаем фактический заголовок Content-Disposition
        String contentDisposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        // Проверяем, что заголовок содержит ожидаемое имя файла
        assertTrue(contentDisposition.contains("file1.txt"));


        Mockito.verify(fileService, Mockito.times(1)).downloadFile(username, filename);
    }

    @Test
    @DisplayName("Should throw InvalidInputException when filename is empty during download")
    void shouldThrowInvalidInputExceptionWhenFilenameIsEmptyDuringDownload() {
        String filename = "";
        // Настройка мока для fileService, чтобы выбросить исключение
        when(fileService.downloadFile(username, filename)).thenThrow(new InvalidInputException("Filename cannot be empty."));
        // Проверка, что при вызове downloadFile выбрасывается ожидаемое исключение
        InvalidInputException exception = assertThrows(InvalidInputException.class, () -> {
            fileController.downloadFile(authToken, filename);
        });

        assertEquals("Filename cannot be empty.", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw InvalidInputException when filename is empty during upload")
    void shouldThrowInvalidInputExceptionWhenFilenameIsEmptyDuringUpload() {
        String filename = ""; // Пустое имя файла
        MultipartFile file = new MockMultipartFile("file", filename, "text/plain", "Some content".getBytes());

        // Настраиваем поведение сервиса, чтобы выбрасывать исключение при пустом имени файла
        Mockito.doThrow(new InvalidInputException("Filename cannot be empty."))
                .when(fileService).uploadFile(username, file);

        // Проверяем, что при вызове контроллера выбрасывается исключение
        InvalidInputException exception = assertThrows(InvalidInputException.class, () -> {
            fileController.uploadFile(authToken, file); // Вызываем метод контроллера
        });

        // Проверяем, что сообщение исключения соответствует ожидаемому
        assertEquals("Filename cannot be empty.", exception.getMessage());
    }

}