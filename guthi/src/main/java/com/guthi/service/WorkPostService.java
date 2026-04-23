package com.guthi.service;

import com.guthi.dto.request.CreateWorkPostRequest;
import com.guthi.dto.response.WorkPostResponse;
import com.guthi.exception.BadRequestException;
import com.guthi.exception.ResourceNotFoundException;
import com.guthi.live.LiveUpdatePublisher;
import com.guthi.model.User;
import com.guthi.model.WorkAcceptance;
import com.guthi.model.WorkPost;
import com.guthi.repository.UserRepository;
import com.guthi.repository.WorkAcceptanceRepository;
import com.guthi.repository.WorkPostRepository;
import com.guthi.security.SecurityUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkPostService {

    private final WorkPostRepository workPostRepository;
    private final WorkAcceptanceRepository acceptanceRepository;
    private final UserRepository userRepository;
    private final LiveUpdatePublisher liveUpdatePublisher;

    public WorkPostResponse create(CreateWorkPostRequest request) {
        User poster = SecurityUtils.requireCurrentUser();
        WorkPost post =
                WorkPost.builder()
                        .posterId(poster.getId())
                        .title(request.getTitle())
                        .description(request.getDescription())
                        .mobileNumber(request.getMobileNumber())
                        .postedAt(Instant.now())
                        .build();
        workPostRepository.save(post);
        WorkPostResponse created = toResponse(post, List.of());
        var postsSnap = listAll();
        liveUpdatePublisher.runAfterCommit(() -> liveUpdatePublisher.publishWorkPosts(postsSnap));
        return created;
    }

    public List<WorkPostResponse> listAll() {
        List<WorkPost> posts = workPostRepository.findAllByOrderByPostedAtDesc();
        if (posts.isEmpty()) {
            return List.of();
        }
        Set<String> postIds = posts.stream().map(WorkPost::getId).collect(Collectors.toSet());
        List<WorkAcceptance> allAcc = acceptanceRepository.findByWorkPostIdIn(postIds);
        Map<String, List<WorkAcceptance>> byPost = new HashMap<>();
        for (WorkAcceptance a : allAcc) {
            byPost.computeIfAbsent(a.getWorkPostId(), k -> new ArrayList<>()).add(a);
        }
        return posts.stream()
                .map(p -> toResponse(p, byPost.getOrDefault(p.getId(), List.of())))
                .toList();
    }

    public WorkPostResponse accept(String postId) {
        User user = SecurityUtils.requireCurrentUser();
        WorkPost post =
                workPostRepository
                        .findById(postId)
                        .orElseThrow(() -> new ResourceNotFoundException("Work post not found"));
        if (post.getPosterId().equals(user.getId())) {
            throw new BadRequestException("Cannot accept your own work post");
        }
        if (acceptanceRepository.existsByWorkPostIdAndAcceptedById(postId, user.getId())) {
            throw new BadRequestException("Already accepted this post");
        }
        WorkAcceptance acceptance =
                WorkAcceptance.builder()
                        .workPostId(postId)
                        .acceptedById(user.getId())
                        .acceptedAt(Instant.now())
                        .build();
        acceptanceRepository.save(acceptance);
        List<WorkAcceptance> accs = acceptanceRepository.findByWorkPostIdOrderByAcceptedAtAsc(postId);
        WorkPost refreshed =
                workPostRepository
                        .findById(postId)
                        .orElseThrow(() -> new ResourceNotFoundException("Work post not found"));
        WorkPostResponse out = toResponse(refreshed, accs);
        var postsSnap = listAll();
        liveUpdatePublisher.runAfterCommit(() -> liveUpdatePublisher.publishWorkPosts(postsSnap));
        return out;
    }

    private WorkPostResponse toResponse(WorkPost p, List<WorkAcceptance> accs) {
        User poster =
                userRepository
                        .findById(p.getPosterId())
                        .orElseThrow(() -> new ResourceNotFoundException("Poster not found"));
        Set<String> accepterIds = accs.stream().map(WorkAcceptance::getAcceptedById).collect(Collectors.toSet());
        Map<String, User> usersById = new HashMap<>();
        if (!accepterIds.isEmpty()) {
            userRepository.findAllById(accepterIds).forEach(u -> usersById.put(u.getId(), u));
        }
        List<String> names =
                accs.stream()
                        .map(
                                a -> {
                                    User u = usersById.get(a.getAcceptedById());
                                    return u != null ? u.getFullName() : "?";
                                })
                        .toList();
        return WorkPostResponse.builder()
                .id(p.getId())
                .posterName(poster.getFullName())
                .title(p.getTitle())
                .description(p.getDescription())
                .mobileNumber(p.getMobileNumber())
                .postedAt(p.getPostedAt())
                .acceptanceCount(accs.size())
                .acceptedByNames(names)
                .build();
    }
}
