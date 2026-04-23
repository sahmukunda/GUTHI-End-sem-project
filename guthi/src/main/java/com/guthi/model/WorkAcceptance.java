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

@Document(collection = "work_acceptances")
@CompoundIndex(name = "post_user", def = "{'workPostId': 1, 'acceptedById': 1}", unique = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkAcceptance {

    @Id private String id;

    private String workPostId;

    private String acceptedById;

    private Instant acceptedAt;
}
