package com.guthi.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DonationPersonalBroadcast {
    BigDecimal myContributionsTotal;
    BigDecimal myPendingClaimsTotal;
    BigDecimal myUsedFromPoolTotal;
    BigDecimal myNetContribution;
}
