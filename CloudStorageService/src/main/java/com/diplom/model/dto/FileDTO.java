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

}
