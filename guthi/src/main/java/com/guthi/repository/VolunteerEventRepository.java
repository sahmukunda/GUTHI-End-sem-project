package com.guthi.repository;

import com.guthi.model.VolunteerEvent;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

// ABSTRACTION: Interface depends on stable MongoRepository contract; implementation hidden from callers
// INHERITANCE: Extends MongoRepository to inherit Spring Data query functionality
public interface VolunteerEventRepository extends MongoRepository<VolunteerEvent, String> {

    List<VolunteerEvent> findByActiveTrueOrderByEventDateAsc();

    List<VolunteerEvent> findAllByOrderByEventDateDesc();
}
