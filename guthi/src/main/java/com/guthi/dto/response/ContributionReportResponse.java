package com.guthi.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ContributionReportResponse {
    BigDecimal totalDonated;
    BigDecimal totalAllocated;
    BigDecimal availablePool;
    /** Donations joined to users, then summed per donor. */
    List<DonorTotal> donors;
    /** Allocations grouped by purpose. */
    List<PurposeTotal> allocationsByPurpose;

    @Value
    @Builder
    public static class DonorTotal {
        String donorId;
        String donorName;
        String donorEmail;
        BigDecimal totalAmount;
        int donationCount;
    }

    @Value
    @Builder
    public static class PurposeTotal {
        String purpose;
        BigDecimal totalAmount;
        int allocationCount;
    }
}
