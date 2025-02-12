package com.diplom.service;

import com.diplom.exception.GeneralServiceException;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.messages.Bucket;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @PostConstruct
    public void ensureBucketExists() {
        try {
            // Проверяем существующие бакеты
            List<Bucket> bucketList = minioClient.listBuckets();
            boolean bucketExists = bucketList.stream()
                    .anyMatch(bucket -> bucket.name().equals(bucketName));

            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Bucket '{}' was created successfully.", bucketName);
            } else {
                log.info("Bucket '{}' already exists.", bucketName);
            }
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error while creating MinIO bucket: {}", e.getMessage(), e);
        }
    }

    public boolean deleteFile(String filename) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .build()
            );
            log.info("✅ Файл удален из MinIO: {}", filename);
            return true;
        } catch (Exception e) {
            log.error("Ошибка при удалении файла из MinIO: {}", filename);
            return false;
        }
    }

    public void uploadFile(String filename, MultipartFile file) {
        try {
            InputStream fileInputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .stream(fileInputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            log.info("✅ Файл загружен в MinIO: {}", filename);
        } catch (Exception e) {
            log.error("Ошибка при загрузке файла в MinIO: {}", filename);
            throw new RuntimeException("Ошибка при загрузке файла в MinIO: " + filename, e);
        }
    }

    public boolean saveFile(MultipartFile file) {
        InputStream fileInputStream = null;
        String filename = file.getOriginalFilename();

        try {
            fileInputStream = file.getInputStream();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .stream(fileInputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return true; // Возвращаем true при успешном сохранении
        } catch (Exception e) {
            log.error("Ошибка при загрузке файла в MinIO {}: {}", filename, e.getMessage());
            return false; // Возвращаем false при ошибке
        } finally {
            // Закрываем InputStream, если он был открыт
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    log.error("Ошибка при закрытии InputStream: {}", e.getMessage());
                }
            }
        }
    }

    public void renameFile(String oldFilename, String newFilename) {
        try {
            // Копирование файла с новым именем
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .source(CopySource.builder()
                                    .bucket(bucketName)
                                    .object(oldFilename)
                                    .build())
                            .bucket(bucketName)
                            .object(newFilename)
                            .build()
            );

            // Удаление старого файла
            deleteFile(oldFilename);

        } catch (Exception e) {
            log.error("Ошибка при переименовании файла в MinIO");
            throw new GeneralServiceException("Ошибка при переименовании файла в MinIO", e);
        }
    }

    public byte[] getFile(String filename) {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(filename)
                        .build())) {

            return stream.readAllBytes();
        } catch (Exception e) {
            log.error("Ошибка при загрузке файла из MinIO: {}", filename);
            throw new GeneralServiceException("Ошибка при загрузке файла из MinIO: " + filename, e);
        }
    }

    // Метод для проверки существования файла
    public boolean fileExists(String filename) {
        try {
            // Проверяем, существует ли объект в указанном бакете
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .build()
            );
            return true; // Если исключение не выброшено, файл существует
        } catch (ErrorResponseException e) {
            // Если объект не найден, возвращаем false
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return false;
            }
            // Логируем другие исключения
            log.error("Ошибка при проверке существования файла {}: {}", filename, e.getMessage());
            throw new RuntimeException("Ошибка при проверке существования файла: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Ошибка при проверке существования файла {}: {}", filename, e.getMessage());
            throw new RuntimeException("Ошибка при проверке существования файла: " + e.getMessage(), e);
        }
    }
}
