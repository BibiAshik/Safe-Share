package com.safeshare.repository;

import com.safeshare.entity.ShareLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {

    Optional<ShareLink> findByToken(String token);

    List<ShareLink> findByFileId(Long fileId);

    /**
     * Finds share links that are:
     * - Revoked (isActive=false) AND revokedAt is older than cutoff (7 days ago)
     * - OR expired (expiryTime is not null and older than cutoff)
     */
    @Query("SELECT sl FROM ShareLink sl WHERE " +
           "(sl.isActive = false AND sl.revokedAt IS NOT NULL AND sl.revokedAt < :cutoff) OR " +
           "(sl.expiryTime IS NOT NULL AND sl.expiryTime < :cutoff)")
    List<ShareLink> findExpiredOrRevokedBefore(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Counts how many share links (of any status) still exist for a given file.
     * Used to decide whether the file can be safely deleted.
     */
    @Query("SELECT COUNT(sl) FROM ShareLink sl WHERE sl.file.id = :fileId")
    long countAllLinksForFile(@Param("fileId") Long fileId);
}
