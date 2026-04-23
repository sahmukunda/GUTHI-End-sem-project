package com.guthi.repository;

import com.guthi.model.Festival;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FestivalRepository extends MongoRepository<Festival, String> {

    List<Festival> findAllByOrderByFestivalDateDesc();
}
