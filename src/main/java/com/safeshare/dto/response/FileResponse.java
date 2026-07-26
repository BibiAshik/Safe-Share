package com.safeshare.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class FileResponse {
    private Long id;
    private String originalFilename;
    private String fileType;
    private Integer currentVersion;
    private Long currentSize;
    private LocalDateTime createdAt;
    private List<ShareLinkResponse> shareLinks;
}
