package com.guthi.repository;

import com.guthi.model.Donation;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DonationRepository extends MongoRepository<Donation, String> {

    boolean existsByDonorIdAndNote(String donorId, String note);
}
