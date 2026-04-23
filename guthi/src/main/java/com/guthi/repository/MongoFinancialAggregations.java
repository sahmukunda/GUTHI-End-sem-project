package com.guthi.repository;

import com.guthi.enums.DonationClaimStatus;
import com.guthi.enums.RequestStatus;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

/**
 * JPQL {@code SUM(...)} replacements for MongoDB. Collection names match {@code @Document} mappings.
 */
@Component
@RequiredArgsConstructor
public class MongoFinancialAggregations {

    private final MongoTemplate mongoTemplate;

    public BigDecimal sumTotalDonated() {
        return sumField("donations", "amount", null);
    }

    public BigDecimal sumTotalAmountByDonorId(String donorId) {
        return sumField("donations", "amount", Criteria.where("donorId").is(donorId));
    }

    public BigDecimal sumTotalAllocated() {
        return sumField("donation_allocations", "amountUsed", null);
    }

    public BigDecimal sumApprovedFundByRequesterId(String requesterId, RequestStatus approved) {
        Criteria c =
                Criteria.where("requesterId")
                        .is(requesterId)
                        .and("status")
                        .is(approved.name())
                        .and("fundAmountRequested")
                        .gt(BigDecimal.ZERO);
        return sumField("community_requests", "fundAmountRequested", c);
    }

    public BigDecimal sumPendingAmountByDonorId(String donorId, DonationClaimStatus status) {
        Criteria c =
                Criteria.where("donorId").is(donorId).and("status").is(status.name());
        return sumField("donation_claims", "amount", c);
    }

    private BigDecimal sumField(String collection, String field, Criteria filter) {
        var group = Aggregation.group().sum(field).as("total");
        Aggregation aggregation =
                filter == null
                        ? Aggregation.newAggregation(group)
                        : Aggregation.newAggregation(Aggregation.match(filter), group);
        AggregationResults<Document> results =
                mongoTemplate.aggregate(aggregation, collection, Document.class);
        Document doc = results.getUniqueMappedResult();
        if (doc == null) {
            return BigDecimal.ZERO;
        }
        return toBigDecimal(doc.get("total"));
    }

    private static BigDecimal toBigDecimal(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v instanceof BigDecimal) {
            return (BigDecimal) v;
        }
        if (v instanceof Decimal128) {
            return ((Decimal128) v).bigDecimalValue();
        }
        if (v instanceof Number) {
            return BigDecimal.valueOf(((Number) v).doubleValue());
        }
        return new BigDecimal(v.toString());
    }
}
