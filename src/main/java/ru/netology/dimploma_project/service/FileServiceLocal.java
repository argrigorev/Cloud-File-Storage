package ru.netology.dimploma_project.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.netology.dimploma_project.dto.FileDto;
import ru.netology.dimploma_project.model.FileEntity;
import ru.netology.dimploma_project.model.User;
import ru.netology.dimploma_project.repository.FileRepository;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileServiceLocal implements FileService {
    private final FileRepository fileRepository;
    private final Path storagePath;

    public FileServiceLocal(FileRepository fileRepository,
                            @Value("${app.storage.path:uploads}") String storageDir) {
        this.fileRepository = fileRepository;
        this.storagePath = Paths.get(storageDir);
        try {
            Files.createDirectories(storagePath);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать папку для хранения файлов", e);
        }
    }

    @Override
    public void uploadFile(String filename, byte[] fileData, User owner) {
        try {
            Path userDir = storagePath.resolve(owner.getUsername());
            Files.createDirectories(userDir);

            Path filePath = userDir.resolve(filename);
            Files.write(filePath, fileData, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            FileEntity fileEntity = new FileEntity();
            fileEntity.setFilename(filename);
            fileEntity.setSize((long) fileData.length);
            fileEntity.setStoragePath(filePath.toString());
            fileEntity.setOwner(owner);

            fileRepository.save(fileEntity);
        } catch (IOException e) {
            throw new IllegalArgumentException("Ошибка при загрузке файла.", e);
        }

    }

    @Override
    public byte[] downloadFile(String filename, User owner) {
        FileEntity fileEntity = fileRepository.findByOwnerAndFilename(owner, filename)
                .orElseThrow(() -> new IllegalArgumentException("Файл не найден."));

        try {
            Path filePath = Paths.get(fileEntity.getStoragePath());
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new IllegalArgumentException("Ошибка при чтении файла.");
        }

    }

    @Override
    public void deleteFile(String filename, User owner) {
        FileEntity fileEntity = fileRepository.findByOwnerAndFilename(owner, filename)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        try {
            Files.deleteIfExists(Paths.get(fileEntity.getStoragePath()));
            fileRepository.delete(fileEntity);
        } catch (IOException e) {
            throw new IllegalArgumentException("Ошибка при удалении файла.");
        }

    }

    @Override
    public void renameFile(String oldFilename, String newFilename, User owner) {
        FileEntity fileEntity = fileRepository.findByOwnerAndFilename(owner, oldFilename)
                .orElseThrow(() -> new IllegalArgumentException("Файл не найден"));

        if (fileRepository.findByOwnerAndFilename(owner, newFilename).isPresent()) {
            throw new IllegalArgumentException("Файл с таким именем уже существует: " + newFilename);
        }

        Path oldPath = Paths.get(fileEntity.getStoragePath());
        Path newPath = oldPath.resolveSibling(newFilename);

        try {
            Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalArgumentException("Ошибка при переименовании файла.");
        }

        fileEntity.setFilename(newFilename);
        fileEntity.setStoragePath(newPath.toString());
        fileRepository.save(fileEntity);
    }

    @Override
    public List<FileDto> getAllFiles(User owner) {
        return fileRepository.findAllByOwner(owner)
                .stream()
                .map(f -> new FileDto(f.getFilename(), f.getSize()))
                .collect(Collectors.toList());
    }
}
