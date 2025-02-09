package com.diplom.repository;

import com.diplom.model.File;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import org.springframework.data.domain.Page;


@Repository
public interface FileRepository extends JpaRepository<File, Long> {

    // Метод для поиска файла по имени и имени пользователя
    Optional<File> findByFilenameAndOwner_Username(String filename, String username);

    // Найти файлы пользователя с пагинацией
    Page<File> findByOwner_Username(String username, Pageable pageable);

    Page<File> findByOwner_Id(Long userId, Pageable pageable);
}