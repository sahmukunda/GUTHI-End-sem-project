package com.guthi.controller;

import com.guthi.config.DevDataLoader;
import com.guthi.service.DonationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
@Profile("dev")
public class DevSeedController {

    private final DevDataLoader devDataLoader;
    private final DonationService donationService;

    @PostMapping("/reseed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> reseed() {
        devDataLoader.forceSeed();
        return ResponseEntity.ok("Demo data reseeded. Log in as priya@guthi.local / ChangeMe123! to see populated stats.");
    }

    /** Adds one donation + one approved fund request for the signed-in user (idempotent). */
    @PostMapping("/seed-my-wallet")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> seedMyWallet() {
        donationService.seedDevWalletForCurrentUser();
        return ResponseEntity.ok(
                "Sample donation (₹15,000) and approved pool use (₹3,500) were added for your account.");
    }
}
