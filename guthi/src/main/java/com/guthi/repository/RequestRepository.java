package com.guthi.repository;

import com.guthi.model.CommunityRequest;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RequestRepository extends MongoRepository<CommunityRequest, String> {

    List<CommunityRequest> findByRequesterIdOrderByCreatedAtDesc(String requesterId);

    List<CommunityRequest> findAllByOrderByCreatedAtDesc();
}
