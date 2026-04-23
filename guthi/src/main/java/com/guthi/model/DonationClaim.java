package com.guthi.model;

import com.guthi.enums.DonationClaimStatus;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "donation_claims")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonationClaim {

    @Id private String id;

    private String donorId;

    private BigDecimal amount;

    private String note;

    @Indexed(unique = true)
    private String upiTransactionId;

    private DonationClaimStatus status;

    private Instant submittedAt;

    private Instant resolvedAt;

    private String resolvedById;
}
