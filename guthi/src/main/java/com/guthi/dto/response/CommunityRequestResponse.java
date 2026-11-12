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
    /** 1 = longest wait among pending requests for the same plot. Null when this row is not in the land queue. */
    Integer landQueueRank;
    /** How many pending requests share this plot. */
    Integer landQueueSize;
    /** 1 = first pending request in the admin priority heap. Null on the member list. */
    Integer queueRank;
    /** How many pending requests are in that heap. */
    Integer queueSize;
    /** True when the 0/1 knapsack put this fund request in the suggested pool plan. */
    Boolean fundPlanSelected;
    /** True when these land dates overlap an already approved rental of the same plot. */
    Boolean landOccupied;
    /** Free, occupied, or a pending-calendar note. Null when this row is not a land request. */
    String landOccupancyNote;
    /** True when greedy interval scheduling kept this pending rental on the free calendar. */
    Boolean landCalendarPick;
    /** Who has been called, who accepted, and who declined. Set after approval when volunteers were requested. */
    VolunteerRosterResponse volunteerRoster;
}
