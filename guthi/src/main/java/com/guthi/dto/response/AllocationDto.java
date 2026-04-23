package com.guthi.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AllocationDto {
    String purpose;
    BigDecimal amountUsed;
    LocalDate date;
}
