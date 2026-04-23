package com.guthi.repository;

import com.guthi.model.VolunteerSignup;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface VolunteerSignupRepository extends MongoRepository<VolunteerSignup, String> {

    long countByEventId(String eventId);

    List<VolunteerSignup> findByEventIdOrderBySignedUpAtAsc(String eventId);

    boolean existsByEventIdAndUserId(String eventId, String userId);
}
