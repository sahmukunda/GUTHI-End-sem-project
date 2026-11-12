package com.guthi.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "volunteer_signups")
@CompoundIndex(name = "event_user", def = "{'eventId': 1, 'userId': 1}", unique = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VolunteerSignup {

    @Id private String id;

    @Indexed
    private String eventId;

    @Indexed
    private String userId;

    /** Name as entered at signup (shown to admins). */
    private String participantName;

    private String mobileNumber;

    private Instant signedUpAt;
}
