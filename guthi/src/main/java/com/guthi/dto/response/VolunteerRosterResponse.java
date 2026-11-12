package com.guthi.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VolunteerRosterResponse {
    int needed;
    List<String> accepted;
    List<String> waiting;
    List<String> declined;
}
