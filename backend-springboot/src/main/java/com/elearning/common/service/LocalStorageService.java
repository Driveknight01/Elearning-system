package com.elearning.common.service;

import com.elearning.common.exception.AppException;
import com.elearning.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/**
 * Stores and manages uploaded images on the local filesystem ({@code ./uploads/})
 * served at {@code /uploads/**}.
 */
@Slf4j
@Service
public class LocalStorageService {

    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024; // 5 MB
    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/jpg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    private final Path uploadRoot;
    private final String baseUrl;

    public LocalStorageService(
            @Value("${app.storage.upload-dir:./uploads}") String uploadDir,
            @Value("${app.storage.base-url:http://localhost:8080/uploads}") String baseUrl
    ) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public String storeImage(MultipartFile file, String subFolder) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Vui lòng chọn tệp ảnh để tải lên");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE, "Ảnh tải lên tối đa 5MB");
        }

        String contentType = file.getContentType();
        String normalizedType = contentType != null ? contentType.trim().toLowerCase() : "";
        String extension = ALLOWED_IMAGE_TYPES.get(normalizedType);
        if (extension == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Chỉ hỗ trợ định dạng ảnh JPG, PNG, WEBP hoặc GIF");
        }

        String safeFolder = subFolder.replaceAll("[^a-zA-Z0-9_-]", "");
        String filename = UUID.randomUUID() + extension;

        try {
            Path targetDir = uploadRoot.resolve(safeFolder).normalize();
            if (!targetDir.startsWith(uploadRoot)) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Đường dẫn lưu trữ không hợp lệ");
            }
            Files.createDirectories(targetDir);

            Path targetFile = targetDir.resolve(filename).normalize();
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return baseUrl + "/" + safeFolder + "/" + filename;
        } catch (IOException ex) {
            log.error("Failed to store uploaded image in folder {}", safeFolder, ex);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    public void deleteByUrlIfLocal(String fileUrl) {
        if (!StringUtils.hasText(fileUrl) || !fileUrl.startsWith(baseUrl + "/")) {
            return;
        }
        String relativePath = fileUrl.substring((baseUrl + "/").length());
        Path target = uploadRoot.resolve(relativePath).normalize();
        if (!target.startsWith(uploadRoot)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            log.warn("Could not delete old local file {}: {}", target, ex.getMessage());
        }
    }
}
