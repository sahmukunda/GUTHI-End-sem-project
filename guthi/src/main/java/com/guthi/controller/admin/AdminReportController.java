package com.guthi.controller.admin;

import com.guthi.dto.response.ContributionReportResponse;
import com.guthi.service.DonationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final DonationService donationService;

    @GetMapping("/contributions")
    public ContributionReportResponse contributions(@RequestParam(required = false) String q) {
        return donationService.contributionReport(q);
    }
}
