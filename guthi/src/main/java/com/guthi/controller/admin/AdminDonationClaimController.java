package com.guthi.controller.admin;

import com.guthi.dto.response.DonationClaimResponse;
import com.guthi.model.User;
import com.guthi.security.SecurityUtils;
import com.guthi.service.DonationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/donation-claims")
@RequiredArgsConstructor
public class AdminDonationClaimController {

    private final DonationService donationService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DonationClaimResponse>> listPending() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(donationService.listPendingClaimsForAdmin());
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DonationClaimResponse> approve(@PathVariable String id) {
        User admin = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(donationService.approveClaim(id, admin));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DonationClaimResponse> reject(@PathVariable String id) {
        User admin = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(donationService.rejectClaim(id, admin));
    }
}
