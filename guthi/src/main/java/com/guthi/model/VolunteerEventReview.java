package com.guthi.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "volunteer_event_reviews")
@CompoundIndex(name = "event_reviewer", def = "{'eventId': 1, 'reviewerId': 1}", unique = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VolunteerEventReview {

    @Id private String id;

    private String eventId;

    private String reviewerId;

    private int rating;

    private String comment;

    private Instant createdAt;
}
