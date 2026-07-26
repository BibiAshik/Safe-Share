package com.safeshare.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class ShareLinkResponse {
    private Long id;
    private String token;
    private String shareUrl;
    private String fileType;
    private LocalDateTime expiryTime;
    private Integer maxDownloads;
    private Integer currentDownloads;
    private Boolean isActive;
    private Boolean watermarkEnabled;
    private Boolean hasPassword;
    private String qrCodeUrl;
    private LocalDateTime createdAt;
}
