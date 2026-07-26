package com.safeshare.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "access_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "share_link_id", nullable = false)
    private ShareLink shareLink;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column
    private String browser;

    @Column
    private String device;

    @Column(name = "accessed_at", nullable = false, updatable = false)
    private LocalDateTime accessedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccessStatus status;

    @Column
    private String reason;

    @PrePersist
    protected void onCreate() {
        accessedAt = LocalDateTime.now();
    }
}
