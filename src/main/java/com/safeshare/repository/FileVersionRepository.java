package com.safeshare.repository;

import com.safeshare.entity.FileVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {

    List<FileVersion> findByFileIdOrderByVersionNumberDesc(Long fileId);

    @Query("SELECT fv FROM FileVersion fv WHERE fv.file.id = :fileId ORDER BY fv.versionNumber DESC LIMIT 1")
    Optional<FileVersion> findLatestByFileId(@Param("fileId") Long fileId);

    @Query("SELECT COALESCE(MAX(fv.versionNumber), 0) FROM FileVersion fv WHERE fv.file.id = :fileId")
    Integer findMaxVersionNumber(@Param("fileId") Long fileId);
}
