package com.guthi.repository;

import com.guthi.model.DonationAllocation;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DonationAllocationRepository extends MongoRepository<DonationAllocation, String> {

    List<DonationAllocation> findAllByOrderByAllocatedAtDesc();
}
