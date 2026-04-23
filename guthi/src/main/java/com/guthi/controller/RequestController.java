package com.guthi.controller;

import com.guthi.dto.request.AdminDecisionRequest;
import com.guthi.dto.request.CreateCommunityRequestDto;
import com.guthi.dto.response.CommunityRequestResponse;
import com.guthi.model.User;
import com.guthi.security.SecurityUtils;
import com.guthi.service.RequestService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    @PostMapping
    public ResponseEntity<CommunityRequestResponse> create(
            @Valid @RequestBody CreateCommunityRequestDto body) {
        return ResponseEntity.ok(requestService.create(body));
    }

    @GetMapping("/my")
    public ResponseEntity<List<CommunityRequestResponse>> myRequests() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(requestService.myRequests());
    }

    /** Admin-only list; path must stay distinct from {@code GET /my} (member list). */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CommunityRequestResponse>> listAll() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(requestService.listAllForAdmin());
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommunityRequestResponse> approve(
            @PathVariable String id, @Valid @RequestBody(required = false) AdminDecisionRequest body) {
        User admin = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(requestService.approve(id, body, admin));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommunityRequestResponse> reject(
            @PathVariable String id, @Valid @RequestBody(required = false) AdminDecisionRequest body) {
        return ResponseEntity.ok(requestService.reject(id, body));
    }
}
