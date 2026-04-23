package com.guthi.repository;

import com.guthi.model.Review;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReviewRepository extends MongoRepository<Review, String> {

    List<Review> findByFestivalIdOrderByCreatedAtDesc(String festivalId);

    boolean existsByFestivalIdAndReviewerId(String festivalId, String reviewerId);
}
