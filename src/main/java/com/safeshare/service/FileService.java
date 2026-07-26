package com.safeshare.service;

import com.safeshare.dto.response.FileResponse;
import com.safeshare.dto.response.FileVersionResponse;
import com.safeshare.entity.FileEntity;
import com.safeshare.entity.FileVersion;
import com.safeshare.entity.User;
import com.safeshare.exception.FileNotFoundException;
import com.safeshare.mapper.FileMapper;
import com.safeshare.mapper.FileVersionMapper;
import com.safeshare.repository.FileRepository;
import com.safeshare.repository.FileVersionRepository;
import com.safeshare.repository.ShareLinkRepository;
import com.safeshare.util.FileTypeValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final FileVersionRepository fileVersionRepository;
    private final ShareLinkRepository shareLinkRepository;
    private final FileMapper fileMapper;
    private final FileVersionMapper fileVersionMapper;
    private final FileTypeValidator fileTypeValidator;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Transactional
    public FileResponse uploadFile(MultipartFile file, User owner) throws IOException {
        if (!fileTypeValidator.isValid(file)) {
            throw new IllegalArgumentException("Invalid file type. Allowed: PDF, JPG, PNG, DOCX, XLS, XLSX, ZIP");
        }

        // Create per-user directory
        Path userDir = Paths.get(uploadDir, owner.getId().toString());
        Files.createDirectories(userDir);

        // Save file to disk with UUID prefix
        String storedFilename = UUID.randomUUID() + "-" + file.getOriginalFilename();
        Path filePath = userDir.resolve(storedFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Create File entity
        FileEntity fileEntity = FileEntity.builder()
                .owner(owner)
                .originalFilename(file.getOriginalFilename())
                .fileType(fileTypeValidator.getFileType(file.getOriginalFilename()))
                .build();
        fileRepository.save(fileEntity);

        // Create first version (v1)
        FileVersion version = FileVersion.builder()
                .file(fileEntity)
                .versionNumber(1)
                .storedFilename(storedFilename)
                .fileSize(file.getSize())
                .storagePath(filePath.toString())
                .build();
        fileVersionRepository.save(version);

        fileEntity.getVersions().add(version);
        return fileMapper.toResponse(fileEntity);
    }

    public Page<FileResponse> listFiles(User owner, String search, Pageable pageable) {
        Page<FileEntity> files;
        if (search != null && !search.isBlank()) {
            files = fileRepository.searchByOwnerAndFilename(owner, search, pageable);
        } else {
            files = fileRepository.findByOwner(owner, pageable);
        }
        return files.map(fileMapper::toResponse);
    }

    @Transactional
    public void deleteFile(Long fileId, User owner) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        if (!file.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("You don't have permission to delete this file");
        }

        // Deactivate all share links
        file.getShareLinks().forEach(link -> link.setIsActive(false));
        shareLinkRepository.saveAll(file.getShareLinks());

        // Delete all version files from disk
        for (FileVersion version : file.getVersions()) {
            try {
                Files.deleteIfExists(Paths.get(version.getStoragePath()));
            } catch (IOException ignored) {
            }
        }

        fileRepository.delete(file);
    }

    @Transactional
    public FileVersionResponse uploadNewVersion(Long fileId, MultipartFile file, User owner) throws IOException {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        if (!fileEntity.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("You don't have permission to modify this file");
        }

        if (!fileTypeValidator.isValid(file)) {
            throw new IllegalArgumentException("Invalid file type. Allowed: PDF, JPG, PNG, DOCX, XLS, XLSX, ZIP");
        }

        // Save to disk
        Path userDir = Paths.get(uploadDir, owner.getId().toString());
        Files.createDirectories(userDir);
        String storedFilename = UUID.randomUUID() + "-" + file.getOriginalFilename();
        Path filePath = userDir.resolve(storedFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Get next version number
        Integer maxVersion = fileVersionRepository.findMaxVersionNumber(fileId);

        FileVersion version = FileVersion.builder()
                .file(fileEntity)
                .versionNumber(maxVersion + 1)
                .storedFilename(storedFilename)
                .fileSize(file.getSize())
                .storagePath(filePath.toString())
                .build();
        fileVersionRepository.save(version);

        return fileVersionMapper.toResponse(version);
    }

    public List<FileVersionResponse> listVersions(Long fileId, User owner) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        if (!file.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("You don't have permission to view this file");
        }

        return fileVersionRepository.findByFileIdOrderByVersionNumberDesc(fileId)
                .stream()
                .map(fileVersionMapper::toResponse)
                .collect(Collectors.toList());
    }

    public FileVersion getVersionForDownload(Long fileId, Long versionId, User owner) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        if (!file.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("You don't have permission to download this file");
        }

        return fileVersionRepository.findById(versionId)
                .filter(v -> v.getFile().getId().equals(fileId))
                .orElseThrow(() -> new FileNotFoundException("Version not found"));
    }

    @Transactional
    public FileVersionResponse revertToVersion(Long fileId, Long versionId, User owner) throws IOException {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        if (!file.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("You don't have permission to modify this file");
        }

        FileVersion oldVersion = fileVersionRepository.findById(versionId)
                .filter(v -> v.getFile().getId().equals(fileId))
                .orElseThrow(() -> new FileNotFoundException("Version not found"));

        // Copy old file to new location (append-only history)
        Path oldPath = Paths.get(oldVersion.getStoragePath());
        Path userDir = Paths.get(uploadDir, owner.getId().toString());
        String newStoredFilename = UUID.randomUUID() + "-revert-" + oldVersion.getStoredFilename();
        Path newPath = userDir.resolve(newStoredFilename);
        Files.copy(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);

        // Create new version with next version number
        Integer maxVersion = fileVersionRepository.findMaxVersionNumber(fileId);

        FileVersion newVersion = FileVersion.builder()
                .file(file)
                .versionNumber(maxVersion + 1)
                .storedFilename(newStoredFilename)
                .fileSize(oldVersion.getFileSize())
                .storagePath(newPath.toString())
                .build();
        fileVersionRepository.save(newVersion);

        return fileVersionMapper.toResponse(newVersion);
    }

    /**
     * Used by public download flow — resolves the latest version for a given file.
     */
    public FileVersion getLatestVersion(Long fileId) {
        return fileVersionRepository.findLatestByFileId(fileId)
                .orElseThrow(() -> new FileNotFoundException("No version found for this file"));
    }
}
