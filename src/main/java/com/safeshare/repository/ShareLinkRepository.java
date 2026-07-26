package com.safeshare.repository;

import com.safeshare.entity.ShareLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {

    Optional<ShareLink> findByToken(String token);

    List<ShareLink> findByFileId(Long fileId);

    @Query("SELECT sl FROM ShareLink sl WHERE " +
           "(sl.isActive = false AND sl.createdAt < :cutoff) OR " +
           "(sl.expiryTime IS NOT NULL AND sl.expiryTime < :cutoff)")
    List<ShareLink> findExpiredOrRevokedBefore(@Param("cutoff") LocalDateTime cutoff);
}
