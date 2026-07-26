package com.safeshare.mapper;

import com.safeshare.dto.response.AccessLogResponse;
import com.safeshare.entity.AccessLog;
import org.springframework.stereotype.Component;

@Component
public class AccessLogMapper {

    public AccessLogResponse toResponse(AccessLog log) {
        return AccessLogResponse.builder()
                .id(log.getId())
                .ipAddress(log.getIpAddress())
                .browser(log.getBrowser())
                .device(log.getDevice())
                .accessedAt(log.getAccessedAt())
                .status(log.getStatus().name())
                .reason(log.getReason())
                .build();
    }
}
