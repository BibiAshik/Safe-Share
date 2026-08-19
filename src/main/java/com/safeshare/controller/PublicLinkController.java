package com.safeshare.controller;

import com.safeshare.dto.request.LinkPasswordRequest;
import com.safeshare.entity.AccessStatus;
import com.safeshare.entity.FileVersion;
import com.safeshare.entity.ShareLink;
import com.safeshare.service.*;
import com.safeshare.util.BotUserAgentFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/public/s")
@RequiredArgsConstructor
@Tag(name = "Public Links", description = "Public file access — no authentication required")
public class PublicLinkController {

    private final DownloadService downloadService;
    private final FileService fileService;
    private final WatermarkService watermarkService;
    private final DocumentPreviewService documentPreviewService;
    private final AccessLogService accessLogService;
    private final BotUserAgentFilter botUserAgentFilter;

    @GetMapping("/{token}")
    @Operation(summary = "Validate a share link and return its status")
    public ResponseEntity<Map<String, Object>> validateLink(
            @PathVariable String token,
            HttpServletRequest request) {
        Map<String, Object> status = downloadService.getLinkStatus(token, request);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/{token}/verify")
    @Operation(summary = "Verify password for a password-protected link")
    public ResponseEntity<Map<String, Object>> verifyPassword(
            @PathVariable String token,
            @Valid @RequestBody LinkPasswordRequest passwordRequest,
            HttpServletRequest request) {
        downloadService.verifyPassword(token, passwordRequest.getPassword(), request);
        downloadService.markPasswordVerified(token, request);

        ShareLink link = downloadService.validateLink(token);
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "message", "Password verified",
                "fileType", link.getFile().getFileType(),
                "fileName", link.getFile().getOriginalFilename()
        ));
    }

    @GetMapping("/{token}/preview")
    @Operation(summary = "Preview a file inline (PDF via iframe, images via img tag)")
    public ResponseEntity<?> previewFile(
            @PathVariable String token,
            HttpServletRequest request) throws IOException {
        ShareLink link = downloadService.validateLink(token);
        downloadService.requirePasswordAccess(link, token, request);

        // Log preview access for real humans only — skip social media preview bots
        boolean isBot = botUserAgentFilter.isBot(request.getHeader("User-Agent"));
        if (!isBot) {
            accessLogService.logAccess(link, request, AccessStatus.SUCCESS, "File previewed");
        }

        FileVersion latestVersion = fileService.getLatestVersion(link.getFile().getId());

        String fileType = link.getFile().getFileType().toLowerCase();
        byte[] fileBytes = Files.readAllBytes(Paths.get(latestVersion.getStoragePath()));

        MediaType mediaType;
        switch (fileType) {
            case "pdf":
                mediaType = MediaType.APPLICATION_PDF;
                break;
            case "jpg":
            case "jpeg":
                mediaType = MediaType.IMAGE_JPEG;
                break;
            case "png":
                mediaType = MediaType.IMAGE_PNG;
                break;
            case "docx":
                String html = documentPreviewService.renderDocxAsHtml(
                        Paths.get(latestVersion.getStoragePath()),
                        link.getFile().getOriginalFilename());
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                        .body(html);
            case "xls":
            case "xlsx":
                String workbookHtml = documentPreviewService.renderWorkbookAsHtml(
                        Paths.get(latestVersion.getStoragePath()),
                        link.getFile().getOriginalFilename());
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                        .body(workbookHtml);
            default:
                return ResponseEntity.ok(Map.of(
                        "status", "NO_PREVIEW",
                        "message", "Preview not available for this file type, please download"
                ));
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(fileBytes);
    }

    @GetMapping("/{token}/download")
    @Operation(summary = "Download a file (with watermark if enabled for PDFs)")
    public ResponseEntity<?> downloadFile(
            @PathVariable String token,
            HttpServletRequest request) throws IOException {
        // Re-validate link on every download attempt (never trust a stale page)
        ShareLink link = downloadService.validateLink(token);
        downloadService.requirePasswordAccess(link, token, request);

        boolean isBot = botUserAgentFilter.isBot(request.getHeader("User-Agent"));
        if (!isBot) {
            // Atomic increment of download count via Redis
            downloadService.incrementDownloadCount(token);

            // Log the successful access
            accessLogService.logAccess(link, request, AccessStatus.SUCCESS, "File downloaded");
        }

        FileVersion latestVersion = fileService.getLatestVersion(link.getFile().getId());
        byte[] fileBytes = Files.readAllBytes(Paths.get(latestVersion.getStoragePath()));

        // Apply watermark if enabled and file is PDF
        if (link.getWatermarkEnabled() && "pdf".equalsIgnoreCase(link.getFile().getFileType())) {
            String accessInfo = "anonymous";
            fileBytes = watermarkService.addWatermark(fileBytes, accessInfo);
        }

        String originalFilename = link.getFile().getOriginalFilename();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalFilename + "\"")
                .contentLength(fileBytes.length)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(fileBytes);
    }
}
