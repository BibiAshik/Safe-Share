package com.safeshare.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class AccessLogResponse {
    private Long id;
    private String ipAddress;
    private String browser;
    private String device;
    private LocalDateTime accessedAt;
    private String status;
    private String reason;
}
