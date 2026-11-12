package com.guthi.repository;

import com.guthi.model.User;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

// ABSTRACTION: UserRepository abstracts MongoDB user queries behind a stable interface
// INHERITANCE: Extends MongoRepository<Entity, String> to inherit Spring Data CRUD operations
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
