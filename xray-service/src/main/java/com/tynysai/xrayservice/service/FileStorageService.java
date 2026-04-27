package com.tynysai.xrayservice.service;

import com.tynysai.xrayservice.exception.FileStorageException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {
    @Value("${app.storage.root}")
    private String storageRoot;

    private Path root;

    @PostConstruct
    public void init() {
        try {
            root = Paths.get(storageRoot).toAbsolutePath().normalize();
            Files.createDirectories(root);
            log.info("X-ray storage initialized at {}", root);
        } catch (IOException e) {
            throw new FileStorageException("Could not initialize storage at " + storageRoot, e);
        }
    }

    public String store(MultipartFile file, UUID ownerId, Long analysisId) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("Uploaded file is empty");
        }
        try {
            String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.bin";
            String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
            String relative = ownerId + "/" + analysisId + ext;
            Path target = root.resolve(relative).normalize();
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return relative;
        } catch (IOException e) {
            throw new FileStorageException("Could not store file", e);
        }
    }

    public byte[] load(String storedPath) {
        try {
            return Files.readAllBytes(resolve(storedPath));
        } catch (IOException e) {
            throw new FileStorageException("Could not read file: " + storedPath, e);
        }
    }

    public void delete(String storedPath) {
        try {
            Files.deleteIfExists(resolve(storedPath));
        } catch (IOException e) {
            log.warn("Could not delete file {}: {}", storedPath, e.getMessage());
        }
    }

    public Path resolve(String storedPath) {
        Path candidate = Paths.get(storedPath);
        return (candidate.isAbsolute() ? candidate : root.resolve(candidate)).normalize();
    }
}
