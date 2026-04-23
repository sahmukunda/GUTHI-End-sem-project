package com.guthi.model;

import com.guthi.enums.RequestStatus;
import com.guthi.enums.RequestType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "community_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityRequest {

    @Id private String id;

    @Indexed
    private String requesterId;

    private RequestType type;

    private String description;

    private String landName;

    private LocalDate rentPeriodFrom;

    private LocalDate rentPeriodTo;

    private BigDecimal fundAmountRequested;

    private Integer peopleRequested;

    private RequestStatus status;

    private String adminNote;

    private Instant createdAt;

    private Instant resolvedAt;
}
