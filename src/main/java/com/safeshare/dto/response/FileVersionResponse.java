package com.safeshare.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class FileVersionResponse {
    private Long id;
    private Integer versionNumber;
    private String storedFilename;
    private Long fileSize;
    private LocalDateTime uploadedAt;
}
