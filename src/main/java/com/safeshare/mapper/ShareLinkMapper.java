package com.safeshare.mapper;

import com.safeshare.dto.response.ShareLinkResponse;
import com.safeshare.entity.ShareLink;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ShareLinkMapper {

    @Value("${app.base-url}")
    private String baseUrl;

    public ShareLinkResponse toResponse(ShareLink link) {
        return ShareLinkResponse.builder()
                .id(link.getId())
                .token(link.getToken())
                .shareUrl(baseUrl + "/share.html?token=" + link.getToken())
                .fileType(link.getFile().getFileType())
                .expiryTime(link.getExpiryTime())
                .maxDownloads(link.getMaxDownloads())
                .currentDownloads(link.getCurrentDownloads())
                .isActive(link.getIsActive())
                .watermarkEnabled(link.getWatermarkEnabled())
                .hasPassword(link.getPasswordHash() != null)
                .qrCodeUrl("/api/links/" + link.getId() + "/qrcode")
                .createdAt(link.getCreatedAt())
                .build();
    }
}
