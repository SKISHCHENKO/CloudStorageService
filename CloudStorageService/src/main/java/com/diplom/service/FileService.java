package com.diplom.service;

import com.diplom.exception.FilesNotFoundException;
import com.diplom.exception.GeneralServiceException;
import com.diplom.exception.InvalidInputException;
import com.diplom.model.File;
import com.diplom.model.User;
import com.diplom.repository.FileRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class FileService {


    private final int pageNumber = 0;
    private final FileRepository fileRepository;
    private final String bucketName;
    private final MinioService minioService;

    @Autowired
    public FileService(FileRepository fileRepository, @Value("${minio.bucket-name}") String bucketName,
                       MinioService minioService) {
        this.fileRepository = fileRepository;
        this.bucketName = bucketName;
        this.minioService = minioService;
    }

    /**
     * Загрузка файла в файловое хранилище
     */
    @Transactional
    public void uploadFile(User user, MultipartFile file) {
        log.info("⚠\uFE0F Загружается файл: {} для пользователя: {}", file.getOriginalFilename(), user.getUsername());
        String filename = file.getOriginalFilename();

        try {
            if (file.isEmpty()) {
                log.error("Файл {} пустой!", filename);
                throw new InvalidInputException("Файл пустой!");
            }

            Optional<File> existingFile = fileRepository.findByFilenameAndOwner_Username(filename, user.getUsername());
            if (existingFile.isPresent()) {
                log.warn("⚠\uFE0F Файл с именем {} уже существует. Заменяем...", filename);
                patchFile(filename, user, file);
                return;
            }

            // Сохраняем файл в MinIO
            boolean saveFile = minioService.saveFile(file);

            if (saveFile) {
                // Создаем запись в таблице File
                File fileRecord = File.builder()
                        .filename(filename)
                        .owner(user)
                        .filePath(bucketName + "/" + filename)
                        .size(file.getSize())
                        .build();
                // Сохраняем запись в базе данных
                fileRepository.save(fileRecord);
            }

        } catch (Exception e) {
            // Если произошла ошибка после сохранения в MinIO, необходимо удалить файл из MinIO
            if (minioService.fileExists(filename)) {
                try {
                    minioService.deleteFile(filename);
                    log.info("Файл {} был удален из MinIO из-за ошибки при загрузке.", filename);
                } catch (Exception deleteException) {
                    log.error("Не удалось удалить файл {} из MinIO после ошибки: {}", filename, deleteException.getMessage());
                }
            }
            log.error("Ошибка при загрузке файла {}: {}", filename, e.getMessage());
            throw new RuntimeException("Ошибка при загрузке файла: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void patchFile(String filename, User user, MultipartFile newFile) {
        log.info("⚠\uFE0FЗаменяем файл: {} для пользователя: {}", filename, user.getUsername());

        File oldFile = fileRepository.findByFilenameAndOwner_Username(filename, user.getUsername())
                .orElseThrow(() -> new FilesNotFoundException("Файл: " + filename + " не найден."));

        try {
            // Удаление старого файла из MinIO
            minioService.deleteFile(filename);

            // Удаление записи о файле из БД
            fileRepository.delete(oldFile);

            // Загрузка нового файла
            minioService.uploadFile(filename, newFile);

            // Сохранение новой записи в БД
            File updatedFile = File.builder()
                    .filename(filename)
                    .owner(user)
                    .filePath(bucketName + "/" + filename)
                    .size(newFile.getSize())
                    .build();

            fileRepository.save(updatedFile);

        } catch (Exception e) {
            log.error("Ошибка при замене файла {}", filename);
            throw new GeneralServiceException("Ошибка при замене файла: " + filename, e);
        }
    }

    @Transactional
    public void deleteFile(User user, String filename) {
        // Находим запись файла в базе данных
        Optional<File> fileRecordOptional = fileRepository.findByFilenameAndOwner_Username(filename, user.getUsername());

        if (fileRecordOptional.isPresent()) {
            File fileRecord = fileRecordOptional.get();

            // Удаляем запись из таблицы File
            fileRepository.delete(fileRecord);

            try {
                // Удаляем файл из MinIO
                boolean deleted = minioService.deleteFile(filename);
                if (!deleted) {
                    // Если удаление не удалось, восстанавливаем запись в базе данных
                    fileRepository.save(fileRecord);
                    log.error("Не удалось удалить файл {} из MinIO. Запись восстановлена в базе данных.", filename);
                    throw new GeneralServiceException("Не удалось удалить файл из MinIO: " + filename);
                }
            } catch (Exception e) {
                // Если возникла ошибка при удалении файла, восстанавливаем запись в базе данных
                fileRepository.save(fileRecord);
                log.error("Ошибка при удалении файла {}. Запись восстановлена в базе данных.", filename, e);
                throw new RuntimeException("Ошибка при удалении файла: " + e.getMessage(), e);
            }
        } else {
            log.error("Файл с именем {} не найден для пользователя {}.", filename, user.getUsername());
            throw new RuntimeException("Файл с именем " + filename + " не найден для пользователя " + user.getUsername() + ".");
        }
    }


    public List<File> listFiles(User user, int limit) {
        if (limit <= 0) {
            log.error("Лимит для списка файлов должен быть > 0");
            throw new InvalidInputException("Лимит для списка файлов должен быть > 0");
        }

        try {
            Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "id"));

            Page<File> filePage = fileRepository.findByOwner_Id(user.getId(), pageable);
            List<File> files = filePage.getContent();

            return files;
        } catch (InvalidInputException | FilesNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка в получение списка файлов для пользователя {}", user.getUsername());
            throw new GeneralServiceException("Ошибка в получение списка файлов для пользователя " + user.getUsername(), e);
        }
    }

    @Transactional
    public void editFileName(User user, String filename, String newFileName) {
        if (newFileName == null || newFileName.trim().isEmpty()) {
            log.error("Имя файла пустое!");
            throw new InvalidInputException("Имя файла пустое!");
        }

        File file = fileRepository.findByFilenameAndOwner_Username(filename, user.getUsername())
                .orElseThrow(() -> new FilesNotFoundException(
                        "Файл с именем " + filename + " не найден для пользователя " + user.getUsername() + ".")
                );

        // Проверяем, существует ли файл с таким именем
        Optional<File> existingFile = fileRepository.findByFilenameAndOwner_Username(newFileName, user.getUsername());
        if (existingFile.isPresent()) {
            log.error("Файл с таким именем уже существует!");
            throw new InvalidInputException("Файл с таким именем уже существует!");
        }

        try {
            // Переименование файла в MinIO
            minioService.renameFile(filename, newFileName);

            // Обновление записи в БД
            file.setFilename(newFileName);
            file.setFilePath(bucketName + "/" + newFileName);
            fileRepository.save(file);
        } catch (Exception e) {
            // При возникновении ошибки откат транзакции
            log.error("Ошибка при изменении имени файла {}: {}", newFileName, e.getMessage());
            throw new GeneralServiceException("Ошибка при изменении имени файла", e);
        }
    }


    public byte[] downloadFile(User user, String filename) {

        File file = fileRepository.findByFilenameAndOwner_Username(filename, user.getUsername())
                .orElseThrow(() -> new FilesNotFoundException("Файл не найден: " + filename));

        // Вызываем MinIO для загрузки файла
        return minioService.getFile(file.getFilename());
    }

}