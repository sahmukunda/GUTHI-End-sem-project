package com.guthi.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DonationSummaryResponse {
    /** All members combined — same for every logged-in user. */
    BigDecimal totalDonated;
    BigDecimal totalAllocated;
    BigDecimal availablePool;
    /** Sum of donations recorded under the current user (0 if not signed in). */
    BigDecimal myContributionsTotal;
    /** Sum of pending UPI donation claims for the current user (0 if not signed in). */
    BigDecimal myPendingClaimsTotal;
    /** Sum of approved fund amounts for requests made by the current user (0 if not signed in). */
    BigDecimal myUsedFromPoolTotal;
    /** Convenience: myContributionsTotal - myUsedFromPoolTotal (can be negative). */
    BigDecimal myNetContribution;
    List<AllocationDto> allocations;
}
