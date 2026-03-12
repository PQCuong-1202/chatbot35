package com.ai.chatbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${file.upload-dir}")
    private String uploadDir;

    public String storeFile(MultipartFile file) throws IOException {
        // Tạo tên file duy nhất
        String originalFileName = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFileName);
        String uniqueFileName = generateUniqueFileName(fileExtension);

        // Tạo thư mục theo ngày
        String dateFolder = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Path datePath = Paths.get(uploadDir, dateFolder);

        // Đảm bảo thư mục tồn tại
        if (!Files.exists(datePath)) {
            Files.createDirectories(datePath);
        }

        // Lưu file
        Path filePath = datePath.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("File saved to: {}", filePath.toString());

        return filePath.toString();
    }

    public boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            log.error("Error deleting file: {}", e.getMessage());
            return false;
        }
    }

    public String getFileUrl(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }

        // Extract relative path from upload directory
        Path fullPath = Paths.get(filePath);
        Path uploadPath = Paths.get(uploadDir);

        try {
            Path relativePath = uploadPath.relativize(fullPath);
            return "/uploads/" + relativePath.toString().replace("\\", "/");
        } catch (IllegalArgumentException e) {
            log.error("Error creating file URL: {}", e.getMessage());
            return "";
        }
    }

    private String generateUniqueFileName(String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("file_%s_%s.%s", timestamp, uuid, extension);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) {
            return "txt";
        }

        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }

        return "txt";
    }
}