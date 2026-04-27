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
import java.util.UUID;

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

    public String storeAvatar(MultipartFile file, UUID userId) {
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

        String relative = userId + "/avatar/avatar." + extension;
        Path target = rootLocation.resolve(relative).normalize();
        try {
            Path avatarDir = target.getParent();
            Files.createDirectories(avatarDir);
            try (var stream = Files.list(avatarDir)) {
                stream.forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return relative;
        } catch (IOException e) {
            throw new FileStorageException("Failed to store avatar", e);
        }
    }

    public byte[] loadFile(String storedPath) {
        try {
            Path path = resolve(storedPath);
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new FileStorageException("Could not read file: " + storedPath, e);
        }
    }

    public void deleteFile(String storedPath) {
        try {
            Files.deleteIfExists(resolve(storedPath));
        } catch (IOException e) {
            log.warn("Could not delete file: {}", storedPath);
        }
    }

    private Path resolve(String storedPath) {
        Path candidate = Paths.get(storedPath);
        Path resolved = (candidate.isAbsolute() ? candidate : rootLocation.resolve(candidate)).normalize();
        if (!resolved.startsWith(rootLocation)) {
            throw new BadRequestException("Access denied: path outside storage directory");
        }
        return resolved;
    }

    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0) return "";
        return filename.substring(lastDot + 1);
    }
}
