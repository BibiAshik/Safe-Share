package com.safeshare.service;

import com.safeshare.dto.request.ShareLinkCreateRequest;
import com.safeshare.dto.response.ShareLinkResponse;
import com.safeshare.entity.FileEntity;
import com.safeshare.entity.ShareLink;
import com.safeshare.entity.User;
import com.safeshare.exception.FileNotFoundException;
import com.safeshare.mapper.ShareLinkMapper;
import com.safeshare.repository.FileRepository;
import com.safeshare.repository.ShareLinkRepository;
import com.safeshare.util.TokenGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShareLinkService {

    private final ShareLinkRepository shareLinkRepository;
    private final FileRepository fileRepository;
    private final ShareLinkMapper shareLinkMapper;
    private final TokenGenerator tokenGenerator;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public ShareLinkResponse createLink(ShareLinkCreateRequest request, User owner) {
        FileEntity file = fileRepository.findById(request.getFileId())
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        if (!file.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("You don't have permission to share this file");
        }

        boolean watermarkEnabled = isPdf(file) && Boolean.TRUE.equals(request.getWatermarkEnabled());

        ShareLink link = ShareLink.builder()
                .file(file)
                .token(tokenGenerator.generateToken())
                .expiryTime(request.getExpiryTime())
                .maxDownloads(request.getMaxDownloads())
                .currentDownloads(0)
                .isActive(true)
                .watermarkEnabled(watermarkEnabled)
                .build();

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            link.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        shareLinkRepository.save(link);

        // Initialize download count in Redis
        redisTemplate.opsForValue().set("download_count:" + link.getToken(), "0");

        return shareLinkMapper.toResponse(link);
    }

    @Transactional
    public ShareLinkResponse updateLink(Long linkId, ShareLinkCreateRequest request, User owner) {
        ShareLink link = shareLinkRepository.findById(linkId)
                .orElseThrow(() -> new FileNotFoundException("Share link not found"));

        if (!link.getFile().getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("You don't have permission to modify this link");
        }

        if (request.getExpiryTime() != null) {
            link.setExpiryTime(request.getExpiryTime());
        }
        link.setMaxDownloads(request.getMaxDownloads());
        if (request.getPassword() != null) {
            if (request.getPassword().isBlank()) {
                link.setPasswordHash(null);
            } else {
                link.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            }
        }
        if (request.getWatermarkEnabled() != null) {
            link.setWatermarkEnabled(isPdf(link.getFile()) && request.getWatermarkEnabled());
        }

        shareLinkRepository.save(link);
        return shareLinkMapper.toResponse(link);
    }

    @Transactional
    public void revokeLink(Long linkId, User owner) {
        ShareLink link = shareLinkRepository.findById(linkId)
                .orElseThrow(() -> new FileNotFoundException("Share link not found"));

        if (!link.getFile().getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("You don't have permission to revoke this link");
        }

        link.setIsActive(false);
        link.setRevokedAt(LocalDateTime.now());
        shareLinkRepository.save(link);
    }

    public ShareLinkResponse getLinkById(Long linkId, User owner) {
        ShareLink link = shareLinkRepository.findById(linkId)
                .orElseThrow(() -> new FileNotFoundException("Share link not found"));

        if (!link.getFile().getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("You don't have permission to view this link");
        }

        return shareLinkMapper.toResponse(link);
    }

    public List<ShareLinkResponse> getLinksForFile(Long fileId, User owner) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        if (!file.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("You don't have permission to view links for this file");
        }

        return shareLinkRepository.findByFileId(fileId).stream()
                .map(shareLinkMapper::toResponse)
                .collect(Collectors.toList());
    }

    private boolean isPdf(FileEntity file) {
        return file.getFileType() != null && file.getFileType().equalsIgnoreCase("pdf");
    }
}
