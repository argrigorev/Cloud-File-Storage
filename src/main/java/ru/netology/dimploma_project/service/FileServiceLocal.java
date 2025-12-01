package ru.netology.dimploma_project.service;

import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private static final Logger logger = LogManager.getLogger(FileServiceLocal.class);

    public FileServiceLocal(FileRepository fileRepository,
                            @Value("${app.storage.path:uploads}") String storageDir) {
        this.fileRepository = fileRepository;
        this.storagePath = Paths.get(storageDir);
        try {
            Files.createDirectories(storagePath);
            logger.info("Каталог хранилища '{}' инициализирован", storagePath);
        } catch (IOException e) {
            logger.error("Ошибка создания каталога '{}'", storagePath, e);
            throw new RuntimeException("Не удалось создать папку для хранения файлов", e);
        }
    }

    @Transactional
    @Override
    public void uploadFile(String filename, byte[] fileData, User owner) {
        logger.info("Загрузка файла '{}' пользователем '{}'", filename, owner.getUsername());

        Path userDir = storagePath.resolve(owner.getUsername());
        Path filePath = userDir.resolve(filename);

        try {
            Files.createDirectories(userDir);
            Files.write(filePath, fileData, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            logger.error("Ошибка при загрузке файла '{}'", filename, e);
            throw new IllegalArgumentException("Ошибка при загрузке файла.", e);
        }

        FileEntity fileEntity = new FileEntity();
        fileEntity.setFilename(filename);
        fileEntity.setSize((long) fileData.length);
        fileEntity.setStoragePath(filePath.toString());
        fileEntity.setOwner(owner);
        try {
            fileRepository.save(fileEntity);
            logger.info("Файл '{}' успешно загружен", filename);
        } catch (Exception dbEx) {
            try {
                Files.deleteIfExists(filePath);
                logger.warn("Откат: удален файл '{}'", filename);
            } catch (IOException rollbackEx) {
                logger.error("Невозможно откатить сохранение файла '{}'", filename, rollbackEx);
            }
            throw dbEx;
        }

    }

    @Override
    public byte[] downloadFile(String filename, User owner) {
        logger.info("Скачивание файла '{}' пользователем '{}'", filename, owner.getUsername());

        FileEntity fileEntity = fileRepository.findByOwnerAndFilename(owner, filename)
                .orElseThrow(() -> {
                    logger.warn("Файл '{}' не найден у пользователя '{}', скачивание невозможно",
                            filename, owner.getUsername());
                    return new IllegalArgumentException("Файл не найден.");
                });

        try {
            Path filePath = Paths.get(fileEntity.getStoragePath());
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            logger.error("Ошибка чтения файла '{}'", filename, e);
            throw new IllegalArgumentException("Ошибка при чтении файла.");
        }
    }

    @Transactional
    @Override
    public void deleteFile(String filename, User owner) {
        logger.info("Удаление файла '{}' пользователем '{}'", filename, owner.getUsername());

        FileEntity fileEntity = fileRepository.findByOwnerAndFilename(owner, filename)
                .orElseThrow(() -> {
                    logger.warn("Файл '{}' не найден у пользователя '{}', удаление невозможно",
                            filename, owner.getUsername());
                    return new IllegalArgumentException("File not found");
                });

        fileRepository.delete(fileEntity);

        try {
            Files.deleteIfExists(Paths.get(fileEntity.getStoragePath()));
            logger.info("Файл '{}' успешно удалён", filename);
        } catch (IOException fsEx) {
            logger.error("Ошибка при удалении файла '{}'", filename, fsEx);

            logger.warn("Откат удаления: восстанавливаю запись файла '{}' в БД", filename);
            fileRepository.save(fileEntity);
            throw new IllegalArgumentException("Ошибка удаления файла на диске.", fsEx);
        }
    }

    @Transactional
    @Override
    public void renameFile(String oldFilename, String newFilename, User owner) {
        logger.info("Переименование файла '{}' → '{}' для пользователя '{}'",
                oldFilename, newFilename, owner.getUsername());

        FileEntity fileEntity = fileRepository.findByOwnerAndFilename(owner, oldFilename)
                .orElseThrow(() -> {
                    logger.warn("Файл '{}' не найден у '{}'", oldFilename, owner.getUsername());
                    return new IllegalArgumentException("Файл не найден");
                });

        if (fileRepository.findByOwnerAndFilename(owner, newFilename).isPresent()) {
            logger.warn("Файл '{}' уже существует у пользователя '{}'", newFilename, owner.getUsername());
            throw new IllegalArgumentException("Файл с таким именем уже существует: " + newFilename);
        }

        Path oldPath = Paths.get(fileEntity.getStoragePath());
        Path newPath = oldPath.resolveSibling(newFilename);

        try {
            Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Файл '{}' успешно переименован в '{}'", oldFilename, newFilename);
        } catch (IOException fsEx) {
            logger.error("Ошибка при переименовании файла '{}' → '{}'", oldFilename, newFilename, fsEx);
            throw new IllegalArgumentException("Ошибка при переименовании файла.");
        }

        fileEntity.setFilename(newFilename);
        fileEntity.setStoragePath(newPath.toString());

        try {
            fileRepository.save(fileEntity);
        } catch (Exception dbEx) {
            try {
                Files.move(newPath, oldPath, StandardCopyOption.REPLACE_EXISTING);
                logger.warn("Откат: файл '{}' возвращён в '{}'", newFilename, oldFilename);
            } catch (IOException rollbackEx) {
                logger.error("Невозможно откатить файловую операцию", rollbackEx);
            }
            throw dbEx;
        }
    }

    @Override
    public List<FileEntity> getAllFiles(User owner) {
        logger.info("Получение списка файлов пользователя '{}'", owner.getUsername());
        return fileRepository.findAllByOwner(owner);
    }
}