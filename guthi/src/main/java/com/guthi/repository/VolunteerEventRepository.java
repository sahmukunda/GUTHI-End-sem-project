package com.guthi.repository;

import com.guthi.model.VolunteerEvent;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface VolunteerEventRepository extends MongoRepository<VolunteerEvent, String> {

    List<VolunteerEvent> findByActiveTrueOrderByEventDateAsc();

    List<VolunteerEvent> findAllByOrderByEventDateDesc();
}
