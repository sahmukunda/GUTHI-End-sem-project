package com.guthi.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Shared MongoDB for integration tests; requires Docker when not disabled. */
@Testcontainers(disabledWithoutDocker = true)
public abstract class MongoSpringBootTestSupport {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");
}
