package ru.netology.dimploma_project.service;

import ru.netology.dimploma_project.model.FileEntity;
import ru.netology.dimploma_project.model.User;

import java.util.List;

public interface FileService {
    void uploadFile(String filename, byte[] fileData, User owner);

    byte[] downloadFile(String filename, User owner);

    void deleteFile(String filename, User owner);

    void renameFile(String oldFilename, String newFilename, User owner);

    List<FileEntity> getAllFiles(User owner);
}
