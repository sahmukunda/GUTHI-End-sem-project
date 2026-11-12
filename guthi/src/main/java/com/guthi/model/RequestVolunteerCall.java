package com.guthi.model;

import com.guthi.enums.VolunteerCallStatus;
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

@Document(collection = "request_volunteer_calls")
@CompoundIndex(name = "request_user", def = "{'requestId': 1, 'userId': 1}", unique = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestVolunteerCall {

    @Id private String id;

    @Indexed
    private String requestId;

    @Indexed
    private String userId;

    private VolunteerCallStatus status;

    /** 1 is the head of the queue. Later numbers were appended after someone declined. */
    private int sequence;

    private Instant calledAt;

    private Instant respondedAt;
}
