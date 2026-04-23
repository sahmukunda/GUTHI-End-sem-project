package com.guthi.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "volunteer_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VolunteerEvent {

    @Id private String id;

    private String title;

    private String description;

    private Integer volunteersRequired;

    private LocalDateTime eventDate;

    private boolean active;
}
