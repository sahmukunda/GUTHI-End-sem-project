package com.guthi.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "donation_allocations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonationAllocation {

    @Id private String id;

    private String purpose;

    private BigDecimal amountUsed;

    private LocalDateTime allocatedAt;

    /** Admin who approved the related request, if any. */
    private String approvedById;
}
