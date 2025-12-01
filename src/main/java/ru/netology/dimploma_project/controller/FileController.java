package ru.netology.dimploma_project.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.dimploma_project.dto.FileDto;
import ru.netology.dimploma_project.model.FileEntity;
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

    private static final Logger logger = LogManager.getLogger(FileServiceLocal.class);

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

        logger.info("UPLOAD попытка");

        if (tokenValue == null || tokenValue.isBlank()) {
            logger.warn("UPLOAD отказ — отсутствует токен");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Missing auth-token header", "id", 401));
        }

        var userOpt = authService.findUserByToken(tokenValue);
        if (userOpt.isEmpty()) {
            logger.warn("UPLOAD отказ — токен недействителен");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid or expired token", "id", 401));
        }
        User user = userOpt.get();

        if (filename == null || filename.isBlank()) {
            logger.warn("UPLOAD отказ — отсутствует filename");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Filename is required", "id", 400));
        }

        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            logger.warn("UPLOAD отказ — неверное имя файла");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid filename", "id", 400));
        }

        if (multipartFile == null || multipartFile.isEmpty()) {
            logger.warn("UPLOAD отказ — пустой файл");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "File is required", "id", 400));
        }

        if (fileRepository.findByOwnerAndFilename(user, filename).isPresent()) {
            logger.warn("UPLOAD отказ — файл '{}' уже существует", filename);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "File already exists", "id", 400));
        }

        try {
            fileService.uploadFile(filename, multipartFile.getBytes(), user);
            logger.info("UPLOAD успех — файл '{}' загружен пользователем '{}'", filename, user.getUsername());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.warn("UPLOAD ошибка: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage(), "id", 400));
        } catch (Exception e) {
            logger.error("UPLOAD ошибка сервера", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Internal server error", "id", 500));
        }
    }

    // удаление файла
    @DeleteMapping(path = "/file")
    public ResponseEntity<?> deleteFile(
            @RequestHeader(name = "auth-token", required = false) String tokenValue,
            @RequestParam("filename") String filename) {

        logger.info("DELETE попытка");

        if (tokenValue == null || tokenValue.isBlank()) {
            logger.warn("DELETE отказ — отсутствует токен");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Missing auth-token header", "id", 401));
        }

        var userOpt = authService.findUserByToken(tokenValue);
        if (userOpt.isEmpty()) {
            logger.warn("DELETE отказ — токен недействителен");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid or expired token", "id", 401));
        }
        User user = userOpt.get();

        if (filename == null || filename.isBlank()) {
            logger.warn("DELETE отказ — пустое имя файла");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Filename is required", "id", 400));
        }

        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            logger.warn("DELETE отказ — неверное имя файла");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid filename", "id", 400));
        }

        try {
            fileService.deleteFile(filename, user);
            logger.info("DELETE успех — файл '{}' удалён", filename);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.warn("DELETE ошибка: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage(), "id", 400));
        } catch (Exception e) {
            logger.error("DELETE ошибка сервера", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Internal server error", "id", 500));
        }
    }

    // скачивание файла
    @GetMapping(path = "/file")
    public ResponseEntity<?> getFile(
            @RequestHeader(name = "auth-token", required = false) String tokenValue,
            @RequestParam("filename") String filename) {

        logger.info("DOWNLOAD попытка");

        if (tokenValue == null || tokenValue.isBlank()) {
            logger.warn("DOWNLOAD отказ — отсутствует токен");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Missing auth-token header", "id", 401));
        }

        var userOpt = authService.findUserByToken(tokenValue);
        if (userOpt.isEmpty()) {
            logger.warn("DOWNLOAD отказ — токен недействителен");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid or expired token", "id", 401));
        }
        User user = userOpt.get();

        if (filename == null || filename.isBlank()) {
            logger.warn("DOWNLOAD отказ — пустое имя файла");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Filename is required", "id", 400));
        }

        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            logger.warn("DOWNLOAD отказ — неверное имя файла");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid filename", "id", 400));
        }

        try {
            byte[] data = fileService.downloadFile(filename, user);
            logger.info("DOWNLOAD успех — '{}' ({} bytes)", filename, data.length);

            String contentType = "application/octet-stream";
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .contentLength(data.length)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(data);
        } catch (IllegalArgumentException e) {
            logger.warn("DOWNLOAD ошибка: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage(), "id", 400));
        } catch (Exception e) {
            logger.error("DOWNLOAD ошибка сервера", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Internal server error", "id", 500));
        }
    }

    // переименование файла
    @PutMapping(path = "/file", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> renameFile(
            @RequestHeader(name = "auth-token", required = false) String tokenValue,
            @RequestParam("filename") String filename,
            @RequestBody Map<String, String> body) {

        logger.info("RENAME попытка");

        if (tokenValue == null || tokenValue.isBlank()) {
            logger.warn("RENAME отказ — отсутствует токен");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Missing auth-token header", "id", 401));
        }

        var userOpt = authService.findUserByToken(tokenValue);
        if (userOpt.isEmpty()) {
            logger.warn("RENAME отказ — недействительный токен");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid or expired token", "id", 401));
        }
        User user = userOpt.get();

        if (filename == null || filename.isBlank()) {
            logger.warn("RENAME отказ — пустое имя файла");
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
            logger.warn("RENAME отказ — отсутствует новое имя");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "New file name is required", "id", 400));
        }
        if (newName.contains("..") || newName.contains("/") || newName.contains("\\")) {
            logger.warn("RENAME отказ — неверное новое имя");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid new filename", "id", 400));
        }

        try {
            fileService.renameFile(filename, newName, user);
            logger.info("RENAME успех — '{}' → '{}'", filename, newName);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.warn("RENAME ошибка: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage(), "id", 400));
        } catch (Exception e) {
            logger.error("RENAME ошибка сервера", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Internal server error", "id", 500));
        }
    }

    //получение всех файлов
    @GetMapping(path = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> listFiles(
            @RequestHeader(name = "auth-token", required = false) String tokenValue,
            @RequestParam(name = "limit", required = false) Integer limit) {

        logger.info("LIST попытка");

        if (tokenValue == null || tokenValue.isBlank()) {
            logger.warn("LIST отказ — отсутствует токен");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Missing auth-token header", "id", 401));
        }

        var userOpt = authService.findUserByToken(tokenValue);
        if (userOpt.isEmpty()) {
            logger.warn("LIST отказ — недействительный токен");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid or expired token", "id", 401));
        }
        User user = userOpt.get();

        try {
            List<FileEntity> files = fileService.getAllFiles(user);
            List<FileDto> all = files.stream()
                    .map(f -> new FileDto(f.getFilename(), f.getSize()))
                    .toList();
            if (limit != null && limit < all.size()) {
                all = all.subList(0, Math.max(0, limit));
            }
            logger.info("LIST успех — {} файлов", all.size());
            return ResponseEntity.ok(all);
        } catch (Exception e) {
            logger.error("LIST ошибка сервера", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Internal server error", "id", 500));
        }
    }
}
