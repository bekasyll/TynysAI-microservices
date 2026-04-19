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
            root = Paths.get(storageRoot);
            Files.createDirectories(root);
            log.info("X-ray storage initialized at {}", root);
        } catch (IOException e) {
            throw new FileStorageException("Could not initialize storage at " + storageRoot, e);
        }
    }

    public String store(MultipartFile file, Long ownerId, Long analysisId) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("Uploaded file is empty");
        }
        try {
            String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.bin";
            String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
            Path dir = root.resolve(ownerId.toString());
            Files.createDirectories(dir);
            Path target = dir.resolve(analysisId + ext);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException e) {
            throw new FileStorageException("Could not store file", e);
        }
    }

    public byte[] load(String path) {
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            throw new FileStorageException("Could not read file: " + path, e);
        }
    }

    public void delete(String path) {
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (IOException e) {
            log.warn("Could not delete file {}: {}", path, e.getMessage());
        }
    }
}
