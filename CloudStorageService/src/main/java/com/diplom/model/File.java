package com.diplom.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Entity
@Table(name = "files")
public class File {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "filename", nullable = false)
    String filename;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner", referencedColumnName = "id")
    User owner; // владелец файла

    @Column(name = "filepath", nullable = false)
    String filePath; // путь к файлу

    @Column(name = "size", nullable = false)
    private long size;

    @CreationTimestamp
    @Column(name = "date_of_upload", nullable = false)
    LocalDateTime dateOfUpload; // время и дата загрузки файла
}