package com.guthi.live;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guthi.dto.response.CommunityRequestResponse;
import com.guthi.dto.response.DonationClaimResponse;
import com.guthi.dto.response.DonationPersonalBroadcast;
import com.guthi.dto.response.DonationPoolBroadcast;
import com.guthi.dto.response.FestivalResponse;
import com.guthi.dto.response.FestivalReviewsPush;
import com.guthi.dto.response.VolunteerCallResponse;
import com.guthi.dto.response.VolunteerEventResponse;
import com.guthi.dto.response.VolunteerSignupsPush;
import com.guthi.dto.response.WorkPostResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
@Slf4j
public class LiveUpdatePublisher {

    public static final String TOPIC_DONATIONS_POOL = "/topic/donations/pool";
    public static final String QUEUE_DONATIONS_PERSONAL = "/queue/donations-personal";
    public static final String TOPIC_ADMIN_DONATION_CLAIMS = "/topic/admin/donation-claims";
    public static final String TOPIC_ADMIN_REQUESTS = "/topic/admin/requests";
    public static final String QUEUE_REQUESTS_MY = "/queue/requests-my";
    public static final String QUEUE_VOLUNTEER_CALLS = "/queue/volunteer-calls";
    public static final String TOPIC_VOLUNTEER_EVENTS = "/topic/volunteer-events";
    public static final String TOPIC_ADMIN_VOLUNTEER_SIGNUPS = "/topic/admin/volunteer-signups";
    public static final String TOPIC_WORK_POSTS = "/topic/work-posts";
    public static final String TOPIC_FESTIVALS = "/topic/festivals";
    public static final String TOPIC_FESTIVAL_REVIEWS = "/topic/festival-reviews";

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    private String toJson(Object payload) throws JsonProcessingException {
        return objectMapper.writeValueAsString(payload);
    }

    /** Never fail the HTTP request if STOMP broadcast or JSON serialization fails. */
    private void safeSendTopic(String destination, Object payload) {
        try {
            messagingTemplate.convertAndSend(destination, toJson(payload));
        } catch (Exception e) {
            log.warn("Live update topic {} failed: {}", destination, e.toString());
        }
    }

    private void safeSendUser(String userEmail, String queueSuffix, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(userEmail, queueSuffix, toJson(payload));
        } catch (Exception e) {
            log.warn("Live update user queue {} for {} failed: {}", queueSuffix, userEmail, e.toString());
        }
    }

    public void runAfterCommit(Runnable runnable) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            runnable.run();
                        }
                    });
        } else {
            runnable.run();
        }
    }

    public void publishDonationPool(DonationPoolBroadcast payload) {
        safeSendTopic(TOPIC_DONATIONS_POOL, payload);
    }

    public void publishDonationPersonal(String userEmail, DonationPersonalBroadcast payload) {
        safeSendUser(userEmail, QUEUE_DONATIONS_PERSONAL, payload);
    }

    public void publishAdminDonationClaims(List<DonationClaimResponse> rows) {
        safeSendTopic(TOPIC_ADMIN_DONATION_CLAIMS, rows);
    }

    public void publishAdminRequests(List<CommunityRequestResponse> rows) {
        safeSendTopic(TOPIC_ADMIN_REQUESTS, rows);
    }

    public void publishUserRequestsMy(String userEmail, List<CommunityRequestResponse> rows) {
        safeSendUser(userEmail, QUEUE_REQUESTS_MY, rows);
    }

    public void publishVolunteerCalls(String userEmail, List<VolunteerCallResponse> rows) {
        safeSendUser(userEmail, QUEUE_VOLUNTEER_CALLS, rows);
    }

    public void publishVolunteerEvents(List<VolunteerEventResponse> rows) {
        safeSendTopic(TOPIC_VOLUNTEER_EVENTS, rows);
    }

    public void publishAdminVolunteerSignups(VolunteerSignupsPush push) {
        safeSendTopic(TOPIC_ADMIN_VOLUNTEER_SIGNUPS, push);
    }

    public void publishWorkPosts(List<WorkPostResponse> rows) {
        safeSendTopic(TOPIC_WORK_POSTS, rows);
    }

    public void publishFestivals(List<FestivalResponse> rows) {
        safeSendTopic(TOPIC_FESTIVALS, rows);
    }

    public void publishFestivalReviews(FestivalReviewsPush push) {
        safeSendTopic(TOPIC_FESTIVAL_REVIEWS, push);
    }
}
