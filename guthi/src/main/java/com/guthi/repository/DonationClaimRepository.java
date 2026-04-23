package com.guthi.repository;

import com.guthi.enums.DonationClaimStatus;
import com.guthi.model.DonationClaim;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DonationClaimRepository extends MongoRepository<DonationClaim, String> {

    boolean existsByUpiTransactionId(String upiTransactionId);

    List<DonationClaim> findByStatusOrderBySubmittedAtAsc(DonationClaimStatus status);
}
