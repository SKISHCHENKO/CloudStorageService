package com.diplom.service;

import com.diplom.exception.FilesNotFoundException;
import com.diplom.exception.GeneralServiceException;
import com.diplom.exception.InvalidInputException;
import com.diplom.model.File;
import com.diplom.model.User;
import com.diplom.model.dto.FileDTO;
import com.diplom.repository.FileRepository;
import com.diplom.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import java.time.format.DateTimeFormatter;


@Service
public class FileService {


    private int pageNumber = 0;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final String bucketName;
    private final MinioService minioService;

    @Autowired
    public FileService(FileRepository fileRepository,UserRepository userRepository,
                       @Value("${minio.bucket-name}") String bucketName, MinioService minioService) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.bucketName = bucketName;
        this.minioService = minioService;
    }

    public void uploadFile(String username, MultipartFile file) {
        System.out.println("\u26A0\uFE0F Загружается файл: " + file.getOriginalFilename() + " для пользователя: " + username);
        try {

            if (file.isEmpty()) {
                throw new InvalidInputException("Файл пустой!");
            }
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

            String filename = file.getOriginalFilename();
            Optional<File> existingFile = fileRepository.findByFilenameAndOwner_Username(filename, user.getUsername());
            if (existingFile.isPresent()) {
                System.out.println("⚠️ Файл с именем " + filename + " уже существует. Заменяем...");
                patchFile(filename, username, file);
                return;
            }

            minioService.saveFile(file);

            // Создаем запись в таблице File
            File fileRecord = File.builder()
                    .filename(filename)
                    .owner(user)
                    .filePath(bucketName + "/" + filename)
                    .size(file.getSize())
                    .build();
            fileRepository.save(fileRecord);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при загрузке файла: " + e.getMessage(), e);
        }
    }

    public void patchFile(String filename, String username, MultipartFile newFile) {
        System.out.println("⚠️Заменяем файл: " + filename + " для пользователя: " + username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь: " + username +" не найден."));

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
            throw new GeneralServiceException("Ошибка при замене файла: " + filename, e);
        }
    }


    public void deleteFile(String username, String filename) {
        try {
            // Удаляем файл из MinIO
            boolean deleted = minioService.deleteFile(filename);
            if (!deleted) {
                throw new GeneralServiceException("Не удалось удалить файл из MinIO: " + filename);
            }

            // Удаляем запись из таблицы File
            Optional<File> fileRecordOptional = fileRepository.findByFilenameAndOwner_Username(filename, username);
            fileRecordOptional.ifPresentOrElse(
                    fileRecord -> fileRepository.delete(fileRecord),
                    () -> {
                        throw new RuntimeException("Файл с именем " + filename + " не найден для пользователя " + username + " .");
                    }
            );
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при удалении файла: " + e.getMessage(), e);
        }
    }


    public List<FileDTO> listFiles(String username, int limit) {
        if (limit <= 0) {
            throw new InvalidInputException("Лимит для списка файлов должен быть > 0");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь: " + username +" не найден."));


        try {
            Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "id"));

            Page<File> filePage = fileRepository.findByOwner_Id(user.getId(), pageable);
            List<File> files = filePage.getContent();

            return files.stream()
                    .map(file -> {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        String formattedDate = file.getDateOfUpload().format(formatter);
                        return new FileDTO(file.getFilename(), (int) file.getSize(), formattedDate);
                    })
                    .toList();
        } catch (InvalidInputException | FilesNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralServiceException("Ошибка в получение списка файлов для пользователя " + user.getUsername(), e);
        }
    }

    public void editFileName(String username, String filename, String newFileName) {
        try {
            if (newFileName == null || newFileName.trim().isEmpty()) {
                throw new InvalidInputException("Имя файла пустое!");
            }
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найлен: " + username));

            File file = fileRepository.findByFilenameAndOwner_Username(filename, user.getUsername() )
                    .orElseThrow(() -> new FilesNotFoundException(
                            "Файл с именем " + filename + " не найден для пользователя " + username + " .")
                    );

            // Проверяем, существует ли файл с таким именем
            Optional<File> existingFile = fileRepository.findByFilenameAndOwner_Username(newFileName, user.getUsername());
            if (existingFile.isPresent()) {
                throw new InvalidInputException("Файл с таким именем уже существует!");
            }

            // Переименование файла в MinIO
            minioService.renameFile(filename, newFileName);

            // Обновление записи в БД
            file.setFilename(newFileName);
            file.setFilePath(bucketName + "/" + newFileName);
            fileRepository.save(file);

        } catch (FilesNotFoundException | InvalidInputException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralServiceException("Ошибка при изменении имени файла", e);
        }
    }

    public byte[] downloadFile(String username, String filename) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + username));

        File file = fileRepository.findByFilenameAndOwner_Username(filename, user.getUsername())
                .orElseThrow(() -> new FilesNotFoundException("Файл не найден: " + filename));

        // Вызываем MinIO для загрузки файла
        return minioService.getFile(file.getFilename());
    }

}