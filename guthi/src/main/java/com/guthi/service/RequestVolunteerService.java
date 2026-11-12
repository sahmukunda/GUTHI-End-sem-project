package com.guthi.service;

import com.guthi.dsa.MemberLinkedQueue;
import com.guthi.dto.response.VolunteerCallResponse;
import com.guthi.dto.response.VolunteerRosterResponse;
import com.guthi.enums.RequestStatus;
import com.guthi.enums.Role;
import com.guthi.enums.VolunteerCallStatus;
import com.guthi.exception.BadRequestException;
import com.guthi.exception.ResourceNotFoundException;
import com.guthi.live.LiveUpdatePublisher;
import com.guthi.model.CommunityRequest;
import com.guthi.model.RequestVolunteerCall;
import com.guthi.model.User;
import com.guthi.repository.RequestRepository;
import com.guthi.repository.RequestVolunteerCallRepository;
import com.guthi.repository.UserRepository;
import com.guthi.security.SecurityUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RequestVolunteerService {

    private final RequestVolunteerCallRepository callRepository;
    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final LiveUpdatePublisher liveUpdatePublisher;

    /** First {@code peopleRequested} community members, in join order, after an admin approves. */
    public List<String> assignIfNeeded(CommunityRequest request) {
        if (!needsVolunteers(request) || request.getStatus() != RequestStatus.APPROVED) {
            return List.of();
        }
        if (!callRepository.findByRequestIdOrderBySequenceAsc(request.getId()).isEmpty()) {
            return List.of();
        }
        List<String> chosen =
                MemberLinkedQueue.of(memberOrder(request.getRequesterId()))
                        .firstAvailable(request.getPeopleRequested(), Set.of());
        Instant now = Instant.now();
        int sequence = 1;
        List<String> called = new ArrayList<>();
        for (String userId : chosen) {
            callRepository.save(
                    RequestVolunteerCall.builder()
                            .requestId(request.getId())
                            .userId(userId)
                            .status(VolunteerCallStatus.CALLED)
                            .sequence(sequence++)
                            .calledAt(now)
                            .build());
            called.add(userId);
        }
        return called;
    }

    public List<VolunteerCallResponse> myCalls() {
        User me = SecurityUtils.requireCurrentUser();
        return callsForUser(me);
    }

    public List<String> accept(String callId) {
        return respond(callId, VolunteerCallStatus.ACCEPTED);
    }

    public List<String> decline(String callId) {
        RequestVolunteerCall call = respondCall(callId, VolunteerCallStatus.DECLINED);
        CommunityRequest request =
                requestRepository
                        .findById(call.getRequestId())
                        .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        List<RequestVolunteerCall> existing = callRepository.findByRequestIdOrderBySequenceAsc(request.getId());
        List<String> active = new ArrayList<>();
        Set<String> used = new HashSet<>();
        int maxSequence = 0;
        for (RequestVolunteerCall row : existing) {
            used.add(row.getUserId());
            maxSequence = Math.max(maxSequence, row.getSequence());
            if (row.getStatus() == VolunteerCallStatus.CALLED
                    || row.getStatus() == VolunteerCallStatus.ACCEPTED
                    || row.getId().equals(call.getId())) {
                active.add(row.getUserId());
            }
        }
        String replacement =
                MemberLinkedQueue.replaceDeclined(active, call.getUserId(), memberOrder(request.getRequesterId()), used);
        List<String> affected = new ArrayList<>();
        affected.add(call.getUserId());
        if (replacement != null) {
            callRepository.save(
                    RequestVolunteerCall.builder()
                            .requestId(request.getId())
                            .userId(replacement)
                            .status(VolunteerCallStatus.CALLED)
                            .sequence(maxSequence + 1)
                            .calledAt(Instant.now())
                            .build());
            affected.add(replacement);
        }
        return affected;
    }

    public void publishForUsers(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        for (String userId : new LinkedHashSet<>(userIds)) {
            userRepository
                    .findById(userId)
                    .ifPresent(
                            user ->
                                    liveUpdatePublisher.publishVolunteerCalls(
                                            user.getEmail(), callsForUser(user)));
        }
    }

    public Map<String, VolunteerRosterResponse> notesFor(List<CommunityRequest> requests) {
        List<String> ids = new ArrayList<>();
        for (CommunityRequest request : requests) {
            if (needsVolunteers(request) && request.getStatus() == RequestStatus.APPROVED && request.getId() != null) {
                ids.add(request.getId());
            }
        }
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<String, List<RequestVolunteerCall>> grouped = new HashMap<>();
        Set<String> userIds = new HashSet<>();
        for (RequestVolunteerCall call : callRepository.findByRequestIdIn(ids)) {
            grouped.computeIfAbsent(call.getRequestId(), key -> new ArrayList<>()).add(call);
            userIds.add(call.getUserId());
        }
        Map<String, String> names = new HashMap<>();
        for (String userId : userIds) {
            userRepository.findById(userId).ifPresent(user -> names.put(userId, user.getFullName()));
        }
        Map<String, VolunteerRosterResponse> notes = new HashMap<>();
        for (CommunityRequest request : requests) {
            if (!ids.contains(request.getId())) {
                continue;
            }
            notes.put(request.getId(), formatNote(request.getPeopleRequested(), grouped.get(request.getId()), names));
        }
        return notes;
    }

    private List<String> respond(String callId, VolunteerCallStatus status) {
        RequestVolunteerCall call = respondCall(callId, status);
        return List.of(call.getUserId());
    }

    private RequestVolunteerCall respondCall(String callId, VolunteerCallStatus status) {
        User me = SecurityUtils.requireCurrentUser();
        RequestVolunteerCall call =
                callRepository
                        .findById(callId)
                        .orElseThrow(() -> new ResourceNotFoundException("Volunteer call not found"));
        if (!me.getId().equals(call.getUserId())) {
            throw new BadRequestException("This volunteer call is for another member");
        }
        if (call.getStatus() != VolunteerCallStatus.CALLED) {
            throw new BadRequestException("You already answered this volunteer call");
        }
        call.setStatus(status);
        call.setRespondedAt(Instant.now());
        return callRepository.save(call);
    }

    private List<VolunteerCallResponse> callsForUser(User user) {
        List<VolunteerCallResponse> rows = new ArrayList<>();
        for (RequestVolunteerCall call : callRepository.findByUserIdOrderByCalledAtDesc(user.getId())) {
            if (call.getStatus() == VolunteerCallStatus.DECLINED) {
                continue;
            }
            requestRepository
                    .findById(call.getRequestId())
                    .ifPresent(request -> rows.add(toResponse(call, request)));
        }
        return rows;
    }

    private VolunteerCallResponse toResponse(RequestVolunteerCall call, CommunityRequest request) {
        User requester = userRepository.findById(request.getRequesterId()).orElse(null);
        String summary = request.getDescription() == null ? "" : request.getDescription();
        if (summary.length() > 180) {
            summary = summary.substring(0, 180) + "…";
        }
        return VolunteerCallResponse.builder()
                .id(call.getId())
                .requestId(request.getId())
                .requesterName(requester == null ? "A community member" : requester.getFullName())
                .type(request.getType())
                .summary(summary)
                .peopleNeeded(request.getPeopleRequested())
                .status(call.getStatus())
                .sequence(call.getSequence())
                .build();
    }

    private List<String> memberOrder(String requesterId) {
        List<User> members = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            if (user.getRole() == Role.USER && user.getId() != null && !user.getId().equals(requesterId)) {
                members.add(user);
            }
        }
        members.sort(
                Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(User::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        List<String> ids = new ArrayList<>();
        for (User member : members) {
            ids.add(member.getId());
        }
        return ids;
    }

    private static boolean needsVolunteers(CommunityRequest request) {
        return request.getPeopleRequested() != null && request.getPeopleRequested() > 0;
    }

    private static VolunteerRosterResponse formatNote(
            int needed, List<RequestVolunteerCall> calls, Map<String, String> names) {
        List<RequestVolunteerCall> rows = calls == null ? List.of() : calls;
        return VolunteerRosterResponse.builder()
                .needed(needed)
                .accepted(namesFor(rows, VolunteerCallStatus.ACCEPTED, names))
                .waiting(namesFor(rows, VolunteerCallStatus.CALLED, names))
                .declined(namesFor(rows, VolunteerCallStatus.DECLINED, names))
                .build();
    }

    private static List<String> namesFor(
            List<RequestVolunteerCall> calls, VolunteerCallStatus status, Map<String, String> names) {
        List<String> found = new ArrayList<>();
        for (RequestVolunteerCall call : calls) {
            if (call.getStatus() == status) {
                found.add(names.getOrDefault(call.getUserId(), "Member"));
            }
        }
        return found;
    }
}
