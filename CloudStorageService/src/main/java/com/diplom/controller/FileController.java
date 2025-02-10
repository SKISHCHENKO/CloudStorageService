package com.diplom.controller;


import com.diplom.exception.ErrorResponse;
import com.diplom.exception.GeneralServiceException;
import com.diplom.model.File;
import com.diplom.model.dto.FileDTO;
import com.diplom.service.FileService;
import lombok.AllArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
public class FileController {


    private final FileService fileService;

    /**
     *  Загрузка файла
     */
    @PostMapping("/file")
    public ResponseEntity<Void> uploadFile(
            @RequestHeader(value = "auth-token", required = true) String authToken,
            @RequestPart("file") MultipartFile file) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        fileService.uploadFile(username, file);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     *  Получение списка файлов у авторизованного пользователя
     */
    @GetMapping("/list")
    public ResponseEntity<List<FileDTO>> listFiles(
            @RequestHeader(value = "auth-token", required = true) String authToken,
            @RequestParam(value = "limit", defaultValue = "10", required = false) int limit) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<FileDTO> files = fileService.listFiles(username, limit);

        return new ResponseEntity<>(files, HttpStatus.OK);
    }

    /**
     *  Удаление файла
     */
    @DeleteMapping("/file")
    public ResponseEntity<Void> deleteFile(
            @RequestHeader(value = "auth-token", required = true) String authToken,
            @RequestParam("filename") String filename) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        fileService.deleteFile(username, filename);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     *  Редактирование имени файла - на фронтенде - новое имя файла задается случайным образом
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
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        fileService.editFileName(username, filename, newFileName);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     *  Загрузка файла
     */
    @GetMapping("/file")
    public ResponseEntity<byte[]> downloadFile(
            @RequestHeader(value = "auth-token", required = true) String authToken,
            @RequestParam("filename") String filename) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        byte[] fileBytes = fileService.downloadFile(username, filename);

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

}