package com.safeshare.repository;

import com.safeshare.entity.AccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {

    Page<AccessLog> findByShareLinkIdOrderByAccessedAtDesc(Long shareLinkId, Pageable pageable);
}
