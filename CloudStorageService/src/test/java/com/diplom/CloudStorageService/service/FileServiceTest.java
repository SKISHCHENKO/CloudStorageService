package com.diplom.CloudStorageService.service;

import com.diplom.exception.InvalidInputException;
import com.diplom.model.File;
import com.diplom.model.User;
import com.diplom.repository.FileRepository;
import com.diplom.repository.UserRepository;
import com.diplom.service.FileService;
import com.diplom.service.MinioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FileServiceTest {

    private User user;
    private MultipartFile testFile;
    private final String testUsername = "testUser";

    @Mock
    private FileRepository fileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MinioService minioService; // Добавьте этот мок

    @InjectMocks
    private FileService fileService;


    @BeforeEach
    void setUp() {
        testFile = new MockMultipartFile("file", "testFile.txt", "text/plain", "Test content".getBytes());

        user = new User(); // Создаем объект User
        user.setUsername("testUser");
        user.setPassword("password123");
        user.setEmail("testuser@example.com");
    }

    @Test
    @DisplayName("Should upload file successfully")
    void shouldUploadFileSuccessfully() {
        // Создаем пользователя
        User user = new User();
        user.setUsername(testUsername);
        user.setId(1L); // Убедитесь, что у пользователя есть ID

        // Создаем тестовый файл
        File testFileEntity = new File();
        testFileEntity.setFilename(testFile.getOriginalFilename());
        testFileEntity.setOwner(user);

        // Настраиваем моки
        when(minioService.saveFile(testFile)).thenReturn(true); // Мок для метода saveFile
        when(fileRepository.save(any(File.class))).thenReturn(testFileEntity); // Мок для сохранения файла

        // Выполняем метод uploadFile и проверяем, что он не выбрасывает исключения
        assertDoesNotThrow(() -> fileService.uploadFile(user, testFile));

        // Проверяем, что метод save был вызван один раз с правильными параметрами
        verify(fileRepository, times(1)).save(argThat(file ->
                file.getFilename().equals(testFile.getOriginalFilename()) &&
                        file.getOwner().equals(user)
        ));
    }


    @Test
    @DisplayName("Should list files successfully for a valid user and limit")
    void shouldListFilesSuccessfully() {
        User user = new User();
        user.setUsername(testUsername);
        user.setId(1L);

        int limit = 5;
        File file = new File();
        file.setOwner(user);
        file.setFilename("testFile.txt");
        file.setDateOfUpload(LocalDateTime.now());
        List<File> files = Collections.singletonList(file);

        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "id"));

        // Настраиваем моки
        when(fileRepository.findByOwner_Id(user.getId(), pageable)).thenReturn(new PageImpl<>(files, pageable, files.size()));

        // Вызываем метод listFiles
        List<File> result = fileService.listFiles(user, limit);

        // Проверяем результаты
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testFile.txt", result.getFirst().getFilename());
    }


    @Test
    @DisplayName("Should throw InvalidInputException when limit is invalid")
    void shouldThrowInvalidInputExceptionWhenLimitIsInvalid() {
        // Устанавливаем некорректный лимит
        int invalidLimit = -1;

        // Проверяем, что при вызове метода listFiles с некорректным лимитом будет выброшено исключение
        InvalidInputException exception = assertThrows(InvalidInputException.class, () -> fileService.listFiles(user, invalidLimit));

        // Проверяем сообщение исключения
        assertEquals("Лимит для списка файлов должен быть > 0", exception.getMessage());
    }


}
