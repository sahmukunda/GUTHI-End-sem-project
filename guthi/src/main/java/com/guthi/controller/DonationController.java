package com.guthi.controller;

import com.guthi.dto.request.CreateDonationClaimRequest;
import com.guthi.dto.response.DonationSummaryResponse;
import com.guthi.service.DonationService;
import com.guthi.service.QrCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/donations")
@RequiredArgsConstructor
public class DonationController {

    private final DonationService donationService;
    private final QrCodeService qrCodeService;

    @GetMapping("/summary")
    public ResponseEntity<DonationSummaryResponse> summary() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(donationService.getSummary());
    }

    @PostMapping("/claims")
    public ResponseEntity<Void> submitClaim(@Valid @RequestBody CreateDonationClaimRequest request) {
        donationService.submitClaim(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Dynamic donation QR when PhonePe is enabled (see {@code guthi.phonepe.*}). Each call uses a
     * new transaction id. Optional {@code amountPaise} overrides the default amount (paise).
     */
    @GetMapping(value = "/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qr(
            @RequestParam(required = false) Long amountPaise,
            @RequestParam(required = false) String message)
            throws Exception {
        return ResponseEntity.ok(qrCodeService.generateDonationQr(amountPaise, message));
    }
}
