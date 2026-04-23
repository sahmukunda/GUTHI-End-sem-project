package com.guthi.dto.response;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VolunteerSignupAdminResponse {
    long count;
    List<SignupRow> signups;

    @Value
    @Builder
    public static class SignupRow {
        String participantName;
        String mobileNumber;
        Instant signedUpAt;
    }
}
