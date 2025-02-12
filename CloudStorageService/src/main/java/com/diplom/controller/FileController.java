package com.diplom.controller;

import com.diplom.exception.ErrorResponse;
import com.diplom.model.File;
import com.diplom.model.User;
import com.diplom.model.dto.FileDTO;
import com.diplom.repository.UserRepository;
import com.diplom.service.FileService;
import lombok.AllArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
public class FileController {


    private final FileService fileService;
    private final UserRepository userRepository;

    /**
     * Загрузка файла в файловое хранилище
     */
    @PostMapping("/file")
    public ResponseEntity<Void> uploadFile(
            @RequestHeader(value = "auth-token", required = true) String authToken,
            @RequestPart("file") MultipartFile file) {

        fileService.uploadFile(loadUser(), file);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Получение списка файлов у авторизованного пользователя
     */
    @GetMapping("/list")
    public ResponseEntity<List<FileDTO>> listFiles(
            @RequestHeader(value = "auth-token", required = true) String authToken,
            @RequestParam(value = "limit", defaultValue = "10", required = false) int limit) {

        try {
            List<File> files = fileService.listFiles(loadUser(), limit);

            if (files.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }

            // Преобразование файлов в DTO
            List<FileDTO> fileDTO = files.stream()
                    .map(file -> {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        String formattedDate = file.getDateOfUpload().format(formatter);
                        return new FileDTO(file.getFilename(), (int) file.getSize(), formattedDate);
                    })
                    .toList();

            // Возврат 200 OK с данными
            return new ResponseEntity<>(fileDTO, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Удаление файла
     */
    @DeleteMapping("/file")
    public ResponseEntity<Void> deleteFile(
            @RequestHeader(value = "auth-token", required = true) String authToken,
            @RequestParam("filename") String filename) {

        fileService.deleteFile(loadUser(), filename);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Редактирование имени файла - на фронтенде - новое имя файла задается случайным образом
     */
    @PutMapping("/file")
    public ResponseEntity<?> editFileName(
            @RequestHeader(value = "auth-token", required = true) String authToken,
            @RequestParam("filename") String filename,
            @RequestBody(required = false) Map<String, String> requestBody) {

        if (requestBody == null || !requestBody.containsKey("filename")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Неправильный запрос на изменение имени файла", 400));
        }

        String newFileName = requestBody.get("filename");

        fileService.editFileName(loadUser(), filename, newFileName);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Скачивание файла из файлового хранилища
     */
    @GetMapping("/file")
    public ResponseEntity<byte[]> downloadFile(
            @RequestHeader(value = "auth-token", required = true) String authToken,
            @RequestParam("filename") String filename) {

        byte[] fileBytes = fileService.downloadFile(loadUser(), filename);

        // Кодируем имя файла в UTF-8 (по RFC 5987)
        String encodedFileName = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replace("+", "%20");

        // Определяем заголовки ответа
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("*=UTF-8''" + encodedFileName, StandardCharsets.UTF_8)
                .build());

        return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
    }

    /**
     * Вспомогательный метод для нахождения текущего авторизованного пользователя.
     */
    public User loadUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}