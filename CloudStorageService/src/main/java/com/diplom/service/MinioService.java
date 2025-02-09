package com.diplom.service;

import com.diplom.exception.GeneralServiceException;
import io.minio.*;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {

    private final MinioClient minioClient; // Бин MinioClient внедряется автоматически

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
    public void deleteFile(String filename) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .build()
            );
            System.out.println("✅ Файл удален из MinIO: " + filename);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при удалении файла из MinIO: " + filename, e);
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
            System.out.println("✅ Файл загружен в MinIO: " + filename);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при загрузке файла в MinIO: " + filename, e);
        }
    }

    public void saveFile(MultipartFile file) throws IOException {

        InputStream fileInputStream = file.getInputStream();
        String filename = file.getOriginalFilename();
        try {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(filename)
                        .stream(fileInputStream, file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при загрузке файла: " + e.getMessage(), e);
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
            throw new GeneralServiceException("Ошибка при загрузке файла из MinIO: " + filename, e);
        }
    }
}
