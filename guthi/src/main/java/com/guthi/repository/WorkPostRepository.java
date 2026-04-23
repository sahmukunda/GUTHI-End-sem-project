package com.guthi.repository;

import com.guthi.model.WorkPost;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface WorkPostRepository extends MongoRepository<WorkPost, String> {

    List<WorkPost> findAllByOrderByPostedAtDesc();
}
