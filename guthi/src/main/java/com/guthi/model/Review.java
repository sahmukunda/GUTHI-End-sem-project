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

@Document(collection = "reviews")
@CompoundIndex(name = "festival_reviewer", def = "{'festivalId': 1, 'reviewerId': 1}", unique = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id private String id;

    private String reviewerId;

    private String festivalId;

    private int rating;

    private String comment;

    private Instant createdAt;
}
