package ru.netology.dimploma_project.dto;

import lombok.*;

@Data
@NoArgsConstructor
public class FileDto {
    private String filename;
    private Long size;

    public FileDto(String filename, Long size) {
        this.filename = filename;
        this.size = size;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}
