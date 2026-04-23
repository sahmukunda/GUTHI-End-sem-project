package com.guthi.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VolunteerSignupsPush {
    String eventId;
    VolunteerSignupAdminResponse payload;
}
