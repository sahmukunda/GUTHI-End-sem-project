package com.guthi.dto.response;

import com.guthi.enums.RequestStatus;
import com.guthi.enums.RequestType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CommunityRequestResponse {
    String id;
    String requesterId;
    String requesterName;
    RequestType type;
    String description;
    String landName;
    LocalDate rentPeriodFrom;
    LocalDate rentPeriodTo;
    BigDecimal fundAmountRequested;
    Integer peopleRequested;
    RequestStatus status;
    String adminNote;
    Instant createdAt;
    Instant resolvedAt;
}
