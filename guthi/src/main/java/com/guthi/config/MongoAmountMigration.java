package com.guthi.config;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * One-time (idempotent) migration: earlier builds stored {@link java.math.BigDecimal} fields as
 * strings, so {@code $sum} aggregations returned 0. We now write them as {@link Decimal128}, but
 * existing documents still hold strings. This runner rewrites those string amounts as Decimal128
 * on boot. It's safe to run repeatedly: documents already numeric are skipped.
 */
@Component
@RequiredArgsConstructor
@Order(1)
@Slf4j
public class MongoAmountMigration implements CommandLineRunner {

    private final MongoTemplate mongoTemplate;

    private static final List<Map.Entry<String, String>> TARGETS =
            List.of(
                    Map.entry("donations", "amount"),
                    Map.entry("donation_claims", "amount"),
                    Map.entry("donation_allocations", "amountUsed"),
                    Map.entry("community_requests", "fundAmountRequested"));

    @Override
    public void run(String... args) {
        for (Map.Entry<String, String> t : TARGETS) {
            convertStringFieldToDecimal128(t.getKey(), t.getValue());
        }
    }

    private void convertStringFieldToDecimal128(String collection, String field) {
        var coll = mongoTemplate.getCollection(collection);
        // $type: 2 == string in BSON
        Document filter = new Document(field, new Document("$type", "string"));
        long stringCount = coll.countDocuments(filter);
        if (stringCount == 0) {
            return;
        }
        log.info(
                "Migrating {} {}.{} values from string → Decimal128 for aggregation-safe sums",
                stringCount,
                collection,
                field);
        try (var cursor = coll.find(filter).iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Object value = doc.get(field);
                if (!(value instanceof String s) || s.isBlank()) {
                    continue;
                }
                try {
                    Decimal128 converted = new Decimal128(new java.math.BigDecimal(s));
                    coll.updateOne(
                            new Document("_id", doc.get("_id")),
                            new Document("$set", new Document(field, converted)));
                } catch (NumberFormatException nfe) {
                    log.warn(
                            "Skipping {}.{} for _id={}: cannot parse '{}' as BigDecimal",
                            collection,
                            field,
                            doc.get("_id"),
                            s);
                }
            }
        }
    }
}
