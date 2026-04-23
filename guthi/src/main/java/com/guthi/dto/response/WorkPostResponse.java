package com.guthi.dto.response;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WorkPostResponse {
    String id;
    String posterName;
    String title;
    String description;
    String mobileNumber;
    Instant postedAt;
    int acceptanceCount;
    List<String> acceptedByNames;
}
