package com.guthi.dto.response;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FestivalResponse {
    String id;
    String name;
    String description;
    LocalDate festivalDate;
    boolean completed;
}
