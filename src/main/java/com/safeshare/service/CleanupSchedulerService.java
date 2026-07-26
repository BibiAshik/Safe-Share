package com.safeshare.service;

import com.safeshare.entity.FileVersion;
import com.safeshare.entity.ShareLink;
import com.safeshare.repository.FileRepository;
import com.safeshare.repository.ShareLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CleanupSchedulerService {

    private final ShareLinkRepository shareLinkRepository;
    private final FileRepository fileRepository;

    /**
     * Runs daily at 2 AM. Finds files whose share links have been expired or revoked
     * for more than 7 days, then deletes all versions from disk and DB.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredFiles() {
        log.info("Starting scheduled cleanup of expired/revoked files");

        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        List<ShareLink> expiredLinks = shareLinkRepository.findExpiredOrRevokedBefore(cutoff);

        Set<Long> fileIdsToDelete = new HashSet<>();
        for (ShareLink link : expiredLinks) {
            fileIdsToDelete.add(link.getFile().getId());
        }

        for (Long fileId : fileIdsToDelete) {
            fileRepository.findById(fileId).ifPresent(file -> {
                for (FileVersion version : file.getVersions()) {
                    try {
                        Files.deleteIfExists(Paths.get(version.getStoragePath()));
                        log.info("Deleted file from disk: {}", version.getStoragePath());
                    } catch (IOException e) {
                        log.error("Failed to delete file: {}", version.getStoragePath(), e);
                    }
                }
                fileRepository.delete(file);
                log.info("Deleted file record: {} (ID: {})", file.getOriginalFilename(), file.getId());
            });
        }

        log.info("Cleanup completed. Processed {} file(s)", fileIdsToDelete.size());
    }
}
