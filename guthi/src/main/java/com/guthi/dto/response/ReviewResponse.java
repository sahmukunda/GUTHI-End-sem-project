package com.guthi.dto.response;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReviewResponse {
    String id;
    String reviewerName;
    int rating;
    String comment;
    Instant createdAt;
}
