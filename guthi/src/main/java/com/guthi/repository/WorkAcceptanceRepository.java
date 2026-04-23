package com.guthi.repository;

import com.guthi.model.WorkAcceptance;
import java.util.Collection;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface WorkAcceptanceRepository extends MongoRepository<WorkAcceptance, String> {

    boolean existsByWorkPostIdAndAcceptedById(String workPostId, String acceptedById);

    List<WorkAcceptance> findByWorkPostIdOrderByAcceptedAtAsc(String workPostId);

    List<WorkAcceptance> findByWorkPostIdIn(Collection<String> workPostIds);
}
