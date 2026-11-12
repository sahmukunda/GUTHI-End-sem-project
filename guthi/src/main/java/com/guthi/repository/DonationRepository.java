package com.guthi.repository;

import com.guthi.model.Donation;
import org.springframework.data.mongodb.repository.MongoRepository;

// ABSTRACTION: DonationRepository is-a interface depending on stable MongoRepository contract
// INHERITANCE: Repository interface extends MongoRepository<Entity, String> for CRUD + custom queries
public interface DonationRepository extends MongoRepository<Donation, String> {

    boolean existsByDonorIdAndNote(String donorId, String note);
}
