package com.safeshare.mapper;

import com.safeshare.dto.response.FileVersionResponse;
import com.safeshare.entity.FileVersion;
import org.springframework.stereotype.Component;

@Component
public class FileVersionMapper {

    public FileVersionResponse toResponse(FileVersion version) {
        return FileVersionResponse.builder()
                .id(version.getId())
                .versionNumber(version.getVersionNumber())
                .storedFilename(version.getStoredFilename())
                .fileSize(version.getFileSize())
                .uploadedAt(version.getUploadedAt())
                .build();
    }
}
