package com.safeshare.util;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Component
public class FileTypeValidator {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "jpg", "jpeg", "png", "docx", "xls", "xlsx", "zip"
    );

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/zip",
            "application/x-zip-compressed"
    );

    public boolean isValid(MultipartFile file) {
        if (file == null || file.isEmpty()) return false;

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) return false;

        String extension = getExtension(filename).toLowerCase();
        String contentType = file.getContentType();

        return ALLOWED_EXTENSIONS.contains(extension) &&
                (contentType == null || ALLOWED_MIME_TYPES.contains(contentType));
    }

    public String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }

    public String getFileType(String filename) {
        return getExtension(filename).toLowerCase();
    }

    public Set<String> getAllowedExtensions() {
        return ALLOWED_EXTENSIONS;
    }
}
