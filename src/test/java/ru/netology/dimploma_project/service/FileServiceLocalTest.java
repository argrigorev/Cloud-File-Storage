package ru.netology.dimploma_project.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.netology.dimploma_project.model.FileEntity;
import ru.netology.dimploma_project.model.User;
import ru.netology.dimploma_project.repository.FileRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FileServiceLocalTest {
    private FileRepository fileRepository;
    private FileServiceLocal fileService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileRepository = mock(FileRepository.class);
        fileService = new FileServiceLocal(fileRepository, tempDir.toString());
    }

    @Test
    void upload_download_list_delete() throws IOException {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("artem");

        byte[] data = "hello world".getBytes();
        String filename = "test.txt";

        when(fileRepository.save(any(FileEntity.class))).thenAnswer(invocationOnMock
                -> invocationOnMock.getArgument(0));

        fileService.uploadFile(filename, data, owner);

        Path userDir = tempDir.resolve(owner.getUsername());
        Path filePath = userDir.resolve(filename);

        assertTrue(Files.exists(filePath), "Uploaded file should exist on disk");
        assertArrayEquals(data, Files.readAllBytes(filePath), "File content should match uploaded bytes");

        FileEntity fileEntity = new FileEntity();
        fileEntity.setFilename(filename);
        fileEntity.setSize((long) data.length);
        fileEntity.setOwner(owner);
        when(fileRepository.findAllByOwner(owner)).thenReturn(List.of(fileEntity));

        var list = fileService.getAllFiles(owner);
        assertEquals(1, list.size(), "There should be one file in list");
        assertEquals(filename, list.get(0).getFilename(), "Filename in list should match");

        fileEntity.setStoragePath(filePath.toString());
        when(fileRepository.findByOwnerAndFilename(owner, filename)).thenReturn(Optional.of(fileEntity));

        byte[] downloaded = fileService.downloadFile(filename, owner);
        assertArrayEquals(data, downloaded, "Downloaded bytes should match original");

        fileService.deleteFile(filename, owner);
        verify(fileRepository).delete(fileEntity);
        assertFalse(Files.exists(filePath), "File should be deleted from disk");
    }

    @Test
    void renameFile_checksExistsAndSave() throws Exception {
        User owner = new User();
        owner.setUsername("artem");

        Path userDir = tempDir.resolve(owner.getUsername());
        Files.createDirectories(userDir);
        Path oldFilePath = userDir.resolve("a.txt");
        Files.write(oldFilePath, "Text for tests".getBytes());

        FileEntity existing = new FileEntity();
        existing.setFilename("a.txt");
        existing.setOwner(owner);
        existing.setStoragePath(oldFilePath.toString());

        when(fileRepository.findByOwnerAndFilename(owner, "a.txt")).thenReturn(Optional.of(existing));
        when(fileRepository.findByOwnerAndFilename(owner, "b.txt")).thenReturn(Optional.empty());
        when(fileRepository.save(any(FileEntity.class))).thenAnswer(i -> i.getArgument(0));

        fileService.renameFile("a.txt", "b.txt", owner);

        assertEquals("b.txt", existing.getFilename(), "Filename should be updated to new name");
        verify(fileRepository).save(existing);

        Path newFilePath = userDir.resolve("b.txt");
        assertFalse(Files.exists(oldFilePath), "Old file should no longer exist");
        assertTrue(Files.exists(newFilePath), "New file should exist after rename");
    }
}
