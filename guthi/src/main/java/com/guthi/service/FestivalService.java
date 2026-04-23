package com.guthi.service;

import com.guthi.dto.request.CreateFestivalRequest;
import com.guthi.dto.request.CreateReviewRequest;
import com.guthi.dto.response.FestivalResponse;
import com.guthi.dto.response.FestivalReviewsPush;
import com.guthi.dto.response.ReviewResponse;
import com.guthi.exception.BadRequestException;
import com.guthi.exception.ResourceNotFoundException;
import com.guthi.live.LiveUpdatePublisher;
import com.guthi.model.Festival;
import com.guthi.model.Review;
import com.guthi.model.User;
import com.guthi.repository.FestivalRepository;
import com.guthi.repository.ReviewRepository;
import com.guthi.repository.UserRepository;
import com.guthi.security.SecurityUtils;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FestivalService {

    private final FestivalRepository festivalRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final LiveUpdatePublisher liveUpdatePublisher;

    public List<FestivalResponse> listFestivals() {
        return festivalRepository.findAllByOrderByFestivalDateDesc().stream()
                .map(this::toFestival)
                .toList();
    }

    public FestivalResponse create(CreateFestivalRequest request) {
        Festival f =
                Festival.builder()
                        .name(request.getName())
                        .description(request.getDescription())
                        .festivalDate(request.getFestivalDate())
                        .completed(false)
                        .build();
        festivalRepository.save(f);
        FestivalResponse out = toFestival(f);
        var festivalsSnap = listFestivals();
        liveUpdatePublisher.runAfterCommit(() -> liveUpdatePublisher.publishFestivals(festivalsSnap));
        return out;
    }

    public FestivalResponse markCompleted(String festivalId) {
        Festival f = findFestival(festivalId);
        f.setCompleted(true);
        festivalRepository.save(f);
        FestivalResponse out = toFestival(f);
        var festivalsSnap = listFestivals();
        liveUpdatePublisher.runAfterCommit(() -> liveUpdatePublisher.publishFestivals(festivalsSnap));
        return out;
    }

    public List<ReviewResponse> listReviews(String festivalId) {
        findFestival(festivalId);
        return reviewRepository.findByFestivalIdOrderByCreatedAtDesc(festivalId).stream()
                .map(this::toReview)
                .toList();
    }

    public ReviewResponse addReview(String festivalId, CreateReviewRequest request) {
        User reviewer = SecurityUtils.requireCurrentUser();
        Festival festival = findFestival(festivalId);
        if (!festival.isCompleted()) {
            throw new BadRequestException(
                    "Reviews are not open yet. A Guthi admin must mark this festival as complete first.");
        }
        if (reviewRepository.existsByFestivalIdAndReviewerId(festivalId, reviewer.getId())) {
            throw new BadRequestException("You already reviewed this festival");
        }
        Review review =
                Review.builder()
                        .reviewerId(reviewer.getId())
                        .festivalId(festivalId)
                        .rating(request.getRating())
                        .comment(request.getComment())
                        .createdAt(Instant.now())
                        .build();
        reviewRepository.save(review);
        ReviewResponse out = toReview(review);
        var reviewsSnap = listReviews(festivalId);
        liveUpdatePublisher.runAfterCommit(
                () ->
                        liveUpdatePublisher.publishFestivalReviews(
                                FestivalReviewsPush.builder()
                                        .festivalId(festivalId)
                                        .reviews(reviewsSnap)
                                        .build()));
        return out;
    }

    private Festival findFestival(String id) {
        return festivalRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Festival not found"));
    }

    private FestivalResponse toFestival(Festival f) {
        return FestivalResponse.builder()
                .id(f.getId())
                .name(f.getName())
                .description(f.getDescription())
                .festivalDate(f.getFestivalDate())
                .completed(f.isCompleted())
                .build();
    }

    private ReviewResponse toReview(Review r) {
        User reviewer =
                userRepository
                        .findById(r.getReviewerId())
                        .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found"));
        return ReviewResponse.builder()
                .id(r.getId())
                .reviewerName(reviewer.getFullName())
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
