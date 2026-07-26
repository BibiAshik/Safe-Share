package com.safeshare.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "share_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "expiry_time")
    private LocalDateTime expiryTime;

    @Column(name = "max_downloads")
    private Integer maxDownloads;

    @Column(name = "current_downloads", nullable = false)
    @Builder.Default
    private Integer currentDownloads = 0;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "watermark_enabled", nullable = false)
    @Builder.Default
    private Boolean watermarkEnabled = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "shareLink", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AccessLog> accessLogs = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
