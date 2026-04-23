package com.guthi.repository;

import com.guthi.model.VolunteerEventReview;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface VolunteerEventReviewRepository extends MongoRepository<VolunteerEventReview, String> {

    List<VolunteerEventReview> findByEventIdOrderByCreatedAtDesc(String eventId);

    boolean existsByEventIdAndReviewerId(String eventId, String reviewerId);
}
