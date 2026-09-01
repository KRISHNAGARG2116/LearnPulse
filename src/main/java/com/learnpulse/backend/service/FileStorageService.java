package com.learnpulse.backend.service;

import com.learnpulse.backend.exception.ApiException;
import com.learnpulse.backend.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    private final Path uploadPath;
    private final long maxSizeBytes;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("pdf", "doc", "docx");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/octet-stream"
    );

    public FileStorageService(
            @Value("${file.upload-dir:uploads/}") String uploadDir,
            @Value("${file.max-size-bytes:20971520}") long maxSizeBytes) {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.maxSizeBytes = maxSizeBytes;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.uploadPath);
            log.info("Local upload storage directory initialized at: {}", this.uploadPath);
        } catch (Exception ex) {
            log.error("Could not create upload directory: {}", this.uploadPath, ex);
            throw new ApiException("Could not initialize local file storage location", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public record StoredFileInfo(
            String originalFileName,
            String storedFileName,
            String filePath,
            long fileSize,
            String contentType
    ) {}

    public StoredFileInfo storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("Cannot store empty or null file", HttpStatus.BAD_REQUEST);
        }

        if (file.getSize() > maxSizeBytes) {
            throw new ApiException("File size exceeds maximum limit of " + (maxSizeBytes / (1024 * 1024)) + " MB", HttpStatus.BAD_REQUEST);
        }

        String rawFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        if (rawFileName.contains("..") || rawFileName.contains("/") || rawFileName.contains("\\")) {
            throw new ApiException("Unsafe filename detected with path traversal sequence: " + rawFileName, HttpStatus.BAD_REQUEST);
        }

        String extension = getFileExtension(rawFileName).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ApiException("Unsupported file extension '." + extension + "'. Allowed formats: PDF, DOC, DOCX", HttpStatus.BAD_REQUEST);
        }

        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            log.warn("MIME type spoofing warning: Reported MIME type '{}' for file '{}'", contentType, rawFileName);
            throw new ApiException("Unsupported file MIME type '" + contentType + "'. Allowed formats: PDF, DOC, DOCX", HttpStatus.BAD_REQUEST);
        }

        String safeBaseName = sanitizeFileName(rawFileName);
        String storedFileName = UUID.randomUUID().toString() + "_" + safeBaseName;

        try {
            Path targetLocation = this.uploadPath.resolve(storedFileName).normalize();

            // Path Traversal Security Check
            if (!targetLocation.startsWith(this.uploadPath)) {
                throw new ApiException("Path traversal attack detected: Cannot store file outside upload directory", HttpStatus.BAD_REQUEST);
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Successfully stored physical file '{}' as '{}' ({}) bytes", rawFileName, storedFileName, file.getSize());

            return new StoredFileInfo(
                    rawFileName,
                    storedFileName,
                    targetLocation.toString(),
                    file.getSize(),
                    contentType != null ? contentType : "application/octet-stream"
            );

        } catch (IOException ex) {
            log.error("Failed to store physical file: {}", rawFileName, ex);
            throw new ApiException("Failed to store physical file: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Resource loadFileAsResource(String storedFileName) {
        try {
            String cleanStoredName = StringUtils.cleanPath(storedFileName);
            if (cleanStoredName.contains("..") || cleanStoredName.contains("/") || cleanStoredName.contains("\\")) {
                throw new ApiException("Invalid stored file name parameter", HttpStatus.BAD_REQUEST);
            }

            Path filePath = this.uploadPath.resolve(cleanStoredName).normalize();
            if (!filePath.startsWith(this.uploadPath)) {
                throw new ApiException("Path traversal attempt detected during file retrieval", HttpStatus.FORBIDDEN);
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Physical file not found or not readable: " + storedFileName);
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("Physical file path is invalid: " + storedFileName);
        }
    }

    public void deleteFile(String storedFileName) {
        try {
            Path filePath = this.uploadPath.resolve(storedFileName).normalize();
            if (filePath.startsWith(this.uploadPath)) {
                Files.deleteIfExists(filePath);
                log.info("Physical file deleted successfully: {}", storedFileName);
            }
        } catch (IOException ex) {
            log.warn("Could not delete physical file: {}", storedFileName, ex);
        }
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1);
    }

    private String sanitizeFileName(String fileName) {
        String nameWithoutExt = fileName;
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex != -1) {
            nameWithoutExt = fileName.substring(0, lastDotIndex);
        }
        String ext = getFileExtension(fileName);

        String sanitized = nameWithoutExt.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }

        return StringUtils.hasText(ext) ? sanitized + "." + ext : sanitized;
    }

    public Path getUploadPath() {
        return uploadPath;
    }
}
