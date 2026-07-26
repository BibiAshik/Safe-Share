package com.safeshare.mapper;

import com.safeshare.dto.response.FileResponse;
import com.safeshare.entity.FileEntity;
import com.safeshare.entity.FileVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FileMapper {

    private final ShareLinkMapper shareLinkMapper;

    public FileResponse toResponse(FileEntity file) {
        FileVersion latestVersion = file.getVersions().isEmpty() ? null : file.getVersions().get(0);

        return FileResponse.builder()
                .id(file.getId())
                .originalFilename(file.getOriginalFilename())
                .fileType(file.getFileType())
                .currentVersion(latestVersion != null ? latestVersion.getVersionNumber() : 0)
                .currentSize(latestVersion != null ? latestVersion.getFileSize() : 0L)
                .createdAt(file.getCreatedAt())
                .shareLinks(file.getShareLinks() != null
                        ? file.getShareLinks().stream().map(shareLinkMapper::toResponse).collect(Collectors.toList())
                        : List.of())
                .build();
    }
}
