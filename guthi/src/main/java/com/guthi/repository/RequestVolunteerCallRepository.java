package com.guthi.repository;

import com.guthi.model.RequestVolunteerCall;
import java.util.Collection;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RequestVolunteerCallRepository extends MongoRepository<RequestVolunteerCall, String> {

    List<RequestVolunteerCall> findByRequestIdOrderBySequenceAsc(String requestId);

    List<RequestVolunteerCall> findByRequestIdIn(Collection<String> requestIds);

    List<RequestVolunteerCall> findByUserIdOrderByCalledAtDesc(String userId);
}
