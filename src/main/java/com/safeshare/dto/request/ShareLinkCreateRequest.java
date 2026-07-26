package com.safeshare.dto.request;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ShareLinkCreateRequest {
    private Long fileId;
    private LocalDateTime expiryTime;
    private Integer maxDownloads;
    private String password;
    private Boolean watermarkEnabled = false;
}
