package com.diplom.model.dto;

import com.diplom.model.File;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.format.DateTimeFormatter;

@Data
@AllArgsConstructor
public class FileDTO {
    private String filename;
    private int size;
    private String editedAt;

    public static FileDTO fromEntity(File file) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return new FileDTO(file.getFilename(), (int) file.getSize(), file.getDateOfUpload().format(formatter));
    }
}
