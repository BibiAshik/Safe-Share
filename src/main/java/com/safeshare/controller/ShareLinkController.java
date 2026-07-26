package com.safeshare.controller;

import com.safeshare.dto.request.ShareLinkCreateRequest;
import com.safeshare.dto.response.AccessLogResponse;
import com.safeshare.dto.response.ShareLinkResponse;
import com.safeshare.security.UserPrincipal;
import com.safeshare.service.AccessLogService;
import com.safeshare.service.QrCodeService;
import com.safeshare.service.ShareLinkService;
import com.google.zxing.WriterException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/links")
@RequiredArgsConstructor
@Tag(name = "Share Links", description = "Create, update, and manage share links")
public class ShareLinkController {

    private final ShareLinkService shareLinkService;
    private final QrCodeService qrCodeService;
    private final AccessLogService accessLogService;

    @PostMapping
    @Operation(summary = "Create a share link for a file")
    public ResponseEntity<ShareLinkResponse> createLink(
            @RequestBody ShareLinkCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ShareLinkResponse response = shareLinkService.createLink(request, principal.getUser());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{linkId}")
    @Operation(summary = "Update share link settings (expiry, max downloads, password, watermark)")
    public ResponseEntity<ShareLinkResponse> updateLink(
            @PathVariable Long linkId,
            @RequestBody ShareLinkCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ShareLinkResponse response = shareLinkService.updateLink(linkId, request, principal.getUser());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{linkId}/revoke")
    @Operation(summary = "Revoke a share link immediately")
    public ResponseEntity<Void> revokeLink(
            @PathVariable Long linkId,
            @AuthenticationPrincipal UserPrincipal principal) {
        shareLinkService.revokeLink(linkId, principal.getUser());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/file/{fileId}")
    @Operation(summary = "List share links for a file")
    public ResponseEntity<List<ShareLinkResponse>> getLinksForFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ShareLinkResponse> links = shareLinkService.getLinksForFile(fileId, principal.getUser());
        return ResponseEntity.ok(links);
    }

    @GetMapping(value = "/{linkId}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Get QR code image for a share link")
    public ResponseEntity<byte[]> getQrCode(
            @PathVariable Long linkId,
            @AuthenticationPrincipal UserPrincipal principal) throws WriterException, IOException {
        ShareLinkResponse link = shareLinkService.getLinkById(linkId, principal.getUser());
        byte[] qrCode = qrCodeService.generateQrCode(link.getShareUrl(), 250, 250);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qrCode);
    }

    @GetMapping("/{linkId}/logs")
    @Operation(summary = "View access logs for a share link (newest first)")
    public ResponseEntity<Page<AccessLogResponse>> getLogs(
            @PathVariable Long linkId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        // Verify ownership
        shareLinkService.getLinkById(linkId, principal.getUser());

        Page<AccessLogResponse> logs = accessLogService.getLogsByLink(
                linkId, PageRequest.of(page, size));
        return ResponseEntity.ok(logs);
    }
}
