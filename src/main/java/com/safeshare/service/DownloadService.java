package com.safeshare.service;

import com.safeshare.entity.AccessStatus;
import com.safeshare.entity.ShareLink;
import com.safeshare.exception.*;
import com.safeshare.repository.ShareLinkRepository;
import com.safeshare.util.BotUserAgentFilter;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class DownloadService {

    private static final String VERIFIED_SHARE_PREFIX = "verified_share:";

    private final ShareLinkRepository shareLinkRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final AccessLogService accessLogService;
    private final BotUserAgentFilter botUserAgentFilter;

    // In-memory Bucket4j rate limiters per token (for password brute-force protection)
    private final Map<String, Bucket> rateLimitBuckets = new ConcurrentHashMap<>();

    /**
     * Validates a share link: checks existence, active status, expiry, and download limit.
     * Throws appropriate exceptions on failure.
     */
    public ShareLink validateLink(String token) {
        ShareLink link = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new FileNotFoundException("Link not found"));

        if (!link.getIsActive()) {
            throw new LinkRevokedException("This link has been revoked");
        }

        if (link.getExpiryTime() != null && LocalDateTime.now().isAfter(link.getExpiryTime())) {
            throw new LinkExpiredException("This link has expired");
        }

        if (link.getMaxDownloads() != null && link.getCurrentDownloads() >= link.getMaxDownloads()) {
            throw new DownloadLimitExceededException("Download limit reached for this link");
        }

        return link;
    }

    /**
     * Returns link status as a map for the public validate endpoint.
     * Does not throw exceptions — returns status strings instead.
     */
    public Map<String, Object> getLinkStatus(String token, HttpServletRequest request) {
        ShareLink link = shareLinkRepository.findByToken(token).orElse(null);

        if (link == null) {
            return Map.of("status", "NOT_FOUND", "message", "Link not found");
        }

        if (!link.getIsActive()) {
            return Map.of("status", "REVOKED", "message", "This link has been revoked");
        }

        if (link.getExpiryTime() != null && LocalDateTime.now().isAfter(link.getExpiryTime())) {
            return Map.of("status", "EXPIRED", "message", "This link has expired");
        }

        if (link.getMaxDownloads() != null && link.getCurrentDownloads() >= link.getMaxDownloads()) {
            return Map.of("status", "LIMIT_REACHED", "message", "Download limit reached for this link");
        }

        if (link.getPasswordHash() != null) {
            return Map.of(
                    "status", "NEEDS_PASSWORD",
                    "message", "This link is password protected",
                    "fileType", link.getFile().getFileType(),
                    "fileName", link.getFile().getOriginalFilename()
            );
        }

        return Map.of(
                "status", "OK",
                "message", "Link is valid",
                "fileType", link.getFile().getFileType(),
                "fileName", link.getFile().getOriginalFilename()
        );
    }

    /**
     * Verifies password for a protected link. Rate-limited via Bucket4j (5 attempts per 10 minutes).
     */
    public boolean verifyPassword(String token, String password, HttpServletRequest request) {
        // Rate limiting: 5 attempts per 10 minutes per token
        Bucket bucket = rateLimitBuckets.computeIfAbsent(token,
                k -> Bucket.builder()
                        .addLimit(Bandwidth.simple(5, Duration.ofMinutes(10)))
                        .build());

        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Too many password attempts. Try again later.");
        }

        ShareLink link = validateLink(token);

        if (link.getPasswordHash() == null) {
            return true; // No password needed
        }

        boolean isBot = botUserAgentFilter.isBot(request.getHeader("User-Agent"));

        if (!passwordEncoder.matches(password, link.getPasswordHash())) {
            if (!isBot) {
                accessLogService.logAccess(link, request, AccessStatus.FAILED, "Wrong password");
            }
            throw new InvalidLinkPasswordException("Incorrect password");
        }

        return true;
    }

    public void markPasswordVerified(String token, HttpServletRequest request) {
        request.getSession(true).setAttribute(VERIFIED_SHARE_PREFIX + token, true);
    }

    public void requirePasswordAccess(ShareLink link, String token, HttpServletRequest request) {
        if (link.getPasswordHash() == null) {
            return;
        }

        Object verified = request.getSession(false) != null
                ? request.getSession(false).getAttribute(VERIFIED_SHARE_PREFIX + token)
                : null;

        if (!Boolean.TRUE.equals(verified)) {
            throw new InvalidLinkPasswordException("Password required");
        }
    }

    /**
     * Atomically increments download count using Redis INCR.
     * Prevents race conditions on simultaneous downloads against the limit.
     */
    public boolean incrementDownloadCount(String token) {
        ShareLink link = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new FileNotFoundException("Link not found"));

        String key = "download_count:" + token;

        // Initialize Redis key if absent
        Boolean exists = redisTemplate.hasKey(key);
        if (exists == null || !exists) {
            redisTemplate.opsForValue().set(key, String.valueOf(link.getCurrentDownloads()));
        }

        // Atomic increment
        Long count = redisTemplate.opsForValue().increment(key);

        if (link.getMaxDownloads() != null && count != null && count > link.getMaxDownloads()) {
            // Rollback
            redisTemplate.opsForValue().decrement(key);
            throw new DownloadLimitExceededException("Download limit reached");
        }

        // Sync count back to database
        link.setCurrentDownloads(count != null ? count.intValue() : link.getCurrentDownloads() + 1);
        shareLinkRepository.save(link);

        return true;
    }

    public boolean isBot(HttpServletRequest request) {
        return botUserAgentFilter.isBot(request.getHeader("User-Agent"));
    }
}
