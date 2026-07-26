package com.safeshare.repository;

import com.safeshare.entity.FileEntity;
import com.safeshare.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FileRepository extends JpaRepository<FileEntity, Long> {

    Page<FileEntity> findByOwner(User owner, Pageable pageable);

    @Query("SELECT f FROM FileEntity f WHERE f.owner = :owner AND LOWER(f.originalFilename) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<FileEntity> searchByOwnerAndFilename(@Param("owner") User owner,
                                               @Param("search") String search,
                                               Pageable pageable);
}
