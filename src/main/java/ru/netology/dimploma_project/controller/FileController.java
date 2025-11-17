package ru.netology.dimploma_project.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.dimploma_project.dto.FileDto;
import ru.netology.dimploma_project.model.User;
import ru.netology.dimploma_project.repository.FileRepository;
import ru.netology.dimploma_project.service.AuthService;
import ru.netology.dimploma_project.service.FileServiceLocal;

import java.util.List;
import java.util.Map;

@RestController
public class FileController {
    private final FileServiceLocal fileService;
    private final FileRepository fileRepository;
    private final AuthService authService;

    public FileController(FileServiceLocal fileService, FileRepository fileRepository, AuthService authService) {
        this.fileService = fileService;
        this.fileRepository = fileRepository;
        this.authService = authService;
    }

    // загрузка файла
    @PostMapping(path = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> uploadFile(
            @RequestHeader(name = "auth-token", required = false) String tokenValue,
            @RequestParam("filename") String filename,
            @RequestPart("file") MultipartFile multipartFile) {

        if (tokenValue == null || tokenValue.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Missing auth-token header", "id", 401));
        }

        var userOpt = authService.findUserByToken(tokenValue);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid or expired token", "id", 401));
        }
        User user = userOpt.get();

        if (filename == null || filename.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Filename is required", "id", 400));
        }

        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid filename", "id", 400));
        }

        if (multipartFile == null || multipartFile.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "File is required", "id", 400));
        }

        if (fileRepository.findByOwnerAndFilename(user, filename).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "File already exists", "id", 400));
        }

        try {
            fileService.uploadFile(filename, multipartFile.getBytes(), user);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage(), "id", 400));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Internal server error", "id", 500));
        }
    }

    // удаление файла
    @DeleteMapping(path = "/file")
    public ResponseEntity<?> deleteFile(
            @RequestHeader(name = "auth-token", required = false) String tokenValue,
            @RequestParam("filename") String filename) {

        if (tokenValue == null || tokenValue.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Missing auth-token header", "id", 401));
        }

        var userOpt = authService.findUserByToken(tokenValue);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid or expired token", "id", 401));
        }
        User user = userOpt.get();

        if (filename == null || filename.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Filename is required", "id", 400));
        }

        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid filename", "id", 400));
        }

        try {
            fileService.deleteFile(filename, user);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage(), "id", 400));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Internal server error", "id", 500));
        }
    }

    // скачивание файла
    @GetMapping(path = "/file")
    public ResponseEntity<?> getFile(
            @RequestHeader(name = "auth-token", required = false) String tokenValue,
            @RequestParam("filename") String filename) {

        if (tokenValue == null || tokenValue.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Missing auth-token header", "id", 401));
        }

        var userOpt = authService.findUserByToken(tokenValue);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid or expired token", "id", 401));
        }
        User user = userOpt.get();

        if (filename == null || filename.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Filename is required", "id", 400));
        }

        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid filename", "id", 400));
        }

        try {
            byte[] data = fileService.downloadFile(filename, user);
            String contentType = "application/octet-stream";
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .contentLength(data.length)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(data);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage(), "id", 400));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Internal server error", "id", 500));
        }
    }

    // переименование файла
    @PutMapping(path = "/file",consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> renameFile(
            @RequestHeader(name = "auth-token", required = false) String tokenValue,
            @RequestParam("filename") String filename,
            @RequestBody Map<String, String> body) {
        if (tokenValue == null || tokenValue.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Missing auth-token header", "id", 401));
        }

        var userOpt = authService.findUserByToken(tokenValue);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid or expired token", "id", 401));
        }
        User user = userOpt.get();

        if (filename == null || filename.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Filename is required", "id", 400));
        }

        String newName = null;
        if (body != null) {
            newName = body.get("name");
            if (newName == null) newName = body.get("filename");
            if (newName == null) newName = body.get("newName");
        }

        if (newName != null) newName = newName.trim();

        if (newName == null || newName.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "New file name is required", "id", 400));
        }
        if (newName.contains("..") || newName.contains("/") || newName.contains("\\")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid new filename", "id", 400));
        }

        try {
            fileService.renameFile(filename, newName, user);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage(), "id", 400));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Internal server error", "id", 500));
        }
    }

    //получение всех файлов
    @GetMapping(path = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> listFiles(
            @RequestHeader(name = "auth-token", required = false) String tokenValue,
            @RequestParam(name = "limit", required = false) Integer limit) {

        if (tokenValue == null || tokenValue.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Missing auth-token header", "id", 401));
        }

        var userOpt = authService.findUserByToken(tokenValue);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid or expired token", "id", 401));
        }
        User user = userOpt.get();

        try {
            List<FileDto> all = fileService.getAllFiles(user);
            if (limit != null && limit < all.size()) {
                all = all.subList(0, Math.max(0, limit));
            }
            return ResponseEntity.ok(all);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Internal server error", "id", 500));
        }
    }
}
