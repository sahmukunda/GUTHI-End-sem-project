package com.guthi.repository;

import com.guthi.model.AuthSession;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuthSessionRepository extends MongoRepository<AuthSession, String> {

    List<AuthSession> findByUserEmailAndRevokedFalse(String userEmail);
}
