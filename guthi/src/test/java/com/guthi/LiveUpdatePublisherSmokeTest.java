package com.guthi;

import com.guthi.dto.response.DonationPoolBroadcast;
import com.guthi.live.LiveUpdatePublisher;
import com.guthi.support.MongoSpringBootTestSupport;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class LiveUpdatePublisherSmokeTest extends MongoSpringBootTestSupport {

    @Autowired private LiveUpdatePublisher liveUpdatePublisher;

    @Test
    void publishDonationPool_runsWithoutException() {
        liveUpdatePublisher.publishDonationPool(
                DonationPoolBroadcast.builder()
                        .totalDonated(BigDecimal.ZERO)
                        .totalAllocated(BigDecimal.ZERO)
                        .availablePool(BigDecimal.ZERO)
                        .allocations(List.of())
                        .build());
    }
}
