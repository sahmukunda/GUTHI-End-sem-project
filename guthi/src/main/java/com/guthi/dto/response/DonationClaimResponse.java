package com.guthi.dto.response;

import com.guthi.enums.DonationClaimStatus;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DonationClaimResponse {
    String id;
    String donorName;
    String donorEmail;
    BigDecimal amount;
    String note;
    String upiTransactionId;
    DonationClaimStatus status;
    Instant submittedAt;
    Instant resolvedAt;
}
