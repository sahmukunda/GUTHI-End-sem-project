package com.guthi.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DonationPoolBroadcast {
    BigDecimal totalDonated;
    BigDecimal totalAllocated;
    BigDecimal availablePool;
    List<AllocationDto> allocations;
}
