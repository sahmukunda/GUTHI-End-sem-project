package com.guthi.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Applies MongoDB JSON Schema validators so inserts and updates are rejected when required fields
 * or types are wrong. Level is moderate: documents already stored are not rewritten.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class MongoSchemaValidator {

    private final MongoTemplate mongoTemplate;

    @Bean
    @Order(100)
    CommandLineRunner applyCollectionValidators() {
        return args -> {
            apply("users", userSchema());
            apply("donations", donationSchema());
            apply("donation_claims", claimSchema());
            apply("donation_allocations", allocationSchema());
            apply("community_requests", requestSchema());
            apply("festivals", festivalSchema());
            apply("reviews", reviewSchema());
            apply("volunteer_events", eventSchema());
            apply("volunteer_signups", signupSchema());
            apply("work_posts", workPostSchema());
            apply("work_acceptances", acceptanceSchema());
            apply("request_volunteer_calls", volunteerCallSchema());
        };
    }

    private void apply(String collection, Document schema) {
        if (!mongoTemplate.collectionExists(collection)) {
            mongoTemplate.createCollection(collection);
        }
        Document command =
                new Document("collMod", collection)
                        .append("validator", new Document("$jsonSchema", schema))
                        .append("validationLevel", "moderate")
                        .append("validationAction", "error");
        try {
            mongoTemplate.getDb().runCommand(command);
            log.info("Schema validator applied to {}", collection);
        } catch (Exception ex) {
            log.warn("Could not apply schema validator to {}: {}", collection, ex.getMessage());
        }
    }

    private static Document userSchema() {
        return required(
                new String[] {"fullName", "email", "password", "role"},
                props()
                        .append("fullName", stringType())
                        .append("email", stringType())
                        .append("password", stringType())
                        .append("mobileNumber", stringType())
                        .append("role", new Document("enum", java.util.List.of("USER", "ADMIN"))));
    }

    private static Document donationSchema() {
        return required(
                new String[] {"donorId", "amount", "donatedAt"},
                props()
                        .append("donorId", stringType())
                        .append("amount", numberType())
                        .append("note", stringType())
                        .append("donatedAt", dateType()));
    }

    private static Document claimSchema() {
        return required(
                new String[] {"donorId", "amount", "upiTransactionId", "status"},
                props()
                        .append("donorId", stringType())
                        .append("amount", numberType())
                        .append("note", stringType())
                        .append("upiTransactionId", stringType())
                        .append("status", new Document("enum", java.util.List.of("PENDING", "APPROVED", "REJECTED"))));
    }

    private static Document allocationSchema() {
        return required(
                new String[] {"purpose", "amountUsed"},
                props()
                        .append("purpose", stringType())
                        .append("amountUsed", numberType())
                        .append("approvedById", stringType()));
    }

    private static Document requestSchema() {
        return required(
                new String[] {"requesterId", "type", "status"},
                props()
                        .append("requesterId", stringType())
                        .append("type", stringType())
                        .append("description", stringType())
                        .append("landName", stringType())
                        .append("status", new Document("enum", java.util.List.of("PENDING", "APPROVED", "REJECTED")))
                        .append("fundAmountRequested", numberType())
                        .append("peopleRequested", new Document("bsonType", java.util.List.of("int", "long", "double", "null"))));
    }

    private static Document festivalSchema() {
        return required(
                new String[] {"name", "festivalDate"},
                props()
                        .append("name", stringType())
                        .append("description", stringType())
                        .append("festivalDate", dateType())
                        .append("completed", new Document("bsonType", java.util.List.of("bool", "null"))));
    }

    private static Document reviewSchema() {
        return required(
                new String[] {"reviewerId", "festivalId", "rating"},
                props()
                        .append("reviewerId", stringType())
                        .append("festivalId", stringType())
                        .append("comment", stringType())
                        .append("rating", new Document("bsonType", java.util.List.of("int", "long", "double")).append("minimum", 1).append("maximum", 5)));
    }

    private static Document eventSchema() {
        return required(
                new String[] {"title", "eventDate"},
                props()
                        .append("title", stringType())
                        .append("description", stringType())
                        .append("volunteersRequired", new Document("bsonType", java.util.List.of("int", "long", "double", "null")))
                        .append("eventDate", dateType())
                        .append("active", new Document("bsonType", java.util.List.of("bool", "null"))));
    }

    private static Document signupSchema() {
        return required(
                new String[] {"eventId", "userId", "participantName"},
                props()
                        .append("eventId", stringType())
                        .append("userId", stringType())
                        .append("participantName", stringType())
                        .append("mobileNumber", stringType()));
    }

    private static Document workPostSchema() {
        return required(
                new String[] {"posterId", "title"},
                props()
                        .append("posterId", stringType())
                        .append("title", stringType())
                        .append("description", stringType())
                        .append("mobileNumber", stringType()));
    }

    private static Document volunteerCallSchema() {
        return required(
                new String[] {"requestId", "userId", "status"},
                props()
                        .append("requestId", stringType())
                        .append("userId", stringType())
                        .append("status", new Document("enum", java.util.List.of("CALLED", "ACCEPTED", "DECLINED")))
                        .append("sequence", new Document("bsonType", java.util.List.of("int", "long", "double", "null"))));
    }

    private static Document acceptanceSchema() {
        return required(
                new String[] {"workPostId", "acceptedById"},
                props().append("workPostId", stringType()).append("acceptedById", stringType()));
    }

    private static Document required(String[] fields, Document properties) {
        return new Document("bsonType", "object")
                .append("required", java.util.List.of(fields))
                .append("properties", properties);
    }

    private static Document props() {
        return new Document();
    }

    private static Document stringType() {
        return new Document("bsonType", java.util.List.of("string", "null"));
    }

    private static Document numberType() {
        return new Document("bsonType", java.util.List.of("decimal", "double", "int", "long", "string", "null"));
    }

    private static Document dateType() {
        return new Document("bsonType", java.util.List.of("date", "string", "null"));
    }
}
