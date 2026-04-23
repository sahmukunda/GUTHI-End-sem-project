package com.guthi.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VolunteerEventResponse {
    String id;
    String title;
    String description;
    int volunteersRequired;
    LocalDateTime eventDate;
    boolean active;
    long currentSignups;
    /** True when the authenticated community member already has a signup for this event. */
    boolean currentUserSignedUp;
    /** {@code true} when the scheduled instant is strictly before now (shift has ended). */
    boolean completed;
    /** Same as {@code completed}: scheduled date/time is strictly before now. */
    boolean schedulePast;
    /** Member may use the signup flow (active and still in the future). */
    boolean signupOpen;
}
