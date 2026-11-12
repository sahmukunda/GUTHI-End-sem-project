package com.guthi.dto.response;

import com.guthi.enums.RequestType;
import com.guthi.enums.VolunteerCallStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VolunteerCallResponse {
    String id;
    String requestId;
    String requesterName;
    RequestType type;
    String summary;
    Integer peopleNeeded;
    VolunteerCallStatus status;
    int sequence;
}
