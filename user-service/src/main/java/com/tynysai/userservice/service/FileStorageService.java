package com.tynysai.userservice.service;

import com.tynysai.userservice.exception.BadRequestException;
import com.tynysai.userservice.exception.FileStorageException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

@Service
@Slf4j
public class FileStorageService {

    @Value("${app.file.upload-dir:./uploads}")
    private String uploadDir;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
            log.info("File storage initialized at: {}", rootLocation);
        } catch (IOException e) {
            throw new FileStorageException("Could not initialize file storage directory", e);
        }
    }

    public String storeAvatar(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty or null");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.contains(".")) {
            throw new BadRequestException("Invalid file name");
        }
        String extension = getExtension(originalName).toLowerCase();
        if (!List.of("jpg", "jpeg", "png").contains(extension)) {
            throw new BadRequestException("Avatar must be JPG or PNG");
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new BadRequestException("Avatar exceeds maximum size of 5MB");
        }

        Path avatarDir = rootLocation.resolve(userId.toString()).resolve("avatar");
        try {
            Files.createDirectories(avatarDir);
            try (var stream = Files.list(avatarDir)) {
                stream.forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
            }
            Path target = avatarDir.resolve("avatar." + extension);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException e) {
            throw new FileStorageException("Failed to store avatar", e);
        }
    }

    public byte[] loadFile(String filePath) {
        try {
            Path path = Paths.get(filePath).normalize();
            if (!path.startsWith(rootLocation)) {
                throw new BadRequestException("Access denied: path outside storage directory");
            }
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new FileStorageException("Could not read file: " + filePath, e);
        }
    }

    public void deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath).normalize();
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Could not delete file: {}", filePath);
        }
    }

    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0) return "";
        return filename.substring(lastDot + 1);
    }
}
