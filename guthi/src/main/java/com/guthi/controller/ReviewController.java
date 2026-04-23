package com.guthi.controller;

import com.guthi.dto.request.CreateReviewRequest;
import com.guthi.dto.response.ReviewResponse;
import com.guthi.service.FestivalService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/festivals")
@RequiredArgsConstructor
public class ReviewController {

    private final FestivalService festivalService;

    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<ReviewResponse>> reviews(@PathVariable String id) {
        return ResponseEntity.ok(festivalService.listReviews(id));
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<ReviewResponse> addReview(
            @PathVariable String id, @Valid @RequestBody CreateReviewRequest body) {
        return ResponseEntity.ok(festivalService.addReview(id, body));
    }
}
