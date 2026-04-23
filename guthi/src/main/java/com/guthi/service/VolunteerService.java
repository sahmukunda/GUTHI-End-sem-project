package com.guthi.service;

import com.guthi.dto.request.CreateVolunteerEventRequest;
import com.guthi.dto.request.VolunteerSignupRequest;
import com.guthi.dto.response.VolunteerEventResponse;
import com.guthi.dto.response.VolunteerSignupsPush;
import com.guthi.dto.response.VolunteerSignupAdminResponse;
import com.guthi.exception.BadRequestException;
import com.guthi.exception.ResourceNotFoundException;
import com.guthi.live.LiveUpdatePublisher;
import com.guthi.model.User;
import com.guthi.model.VolunteerEvent;
import com.guthi.model.VolunteerSignup;
import com.guthi.repository.UserRepository;
import com.guthi.repository.VolunteerEventRepository;
import com.guthi.repository.VolunteerSignupRepository;
import com.guthi.security.SecurityUtils;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VolunteerService {

    private final VolunteerEventRepository eventRepository;
    private final VolunteerSignupRepository signupRepository;
    private final UserRepository userRepository;
    private final LiveUpdatePublisher liveUpdatePublisher;

    public List<VolunteerEventResponse> listEventsForPortal() {
        syncAllActiveFlagsFromSchedule();
        return eventRepository.findAllByOrderByEventDateDesc().stream()
                .map(this::toEventResponse)
                .toList();
    }

    /**
     * {@code active} means the event is still upcoming ({@code eventDate} is strictly after "now"). Past or
     * current-moment events are stored inactive so signups and UI stay consistent.
     */
    private void syncAllActiveFlagsFromSchedule() {
        LocalDateTime now = LocalDateTime.now();
        for (VolunteerEvent e : eventRepository.findAllByOrderByEventDateDesc()) {
            boolean desiredActive = e.getEventDate().isAfter(now);
            if (e.isActive() != desiredActive) {
                e.setActive(desiredActive);
                eventRepository.save(e);
            }
        }
    }

    private void syncActiveFlagForEvent(VolunteerEvent e) {
        LocalDateTime now = LocalDateTime.now();
        boolean desiredActive = e.getEventDate().isAfter(now);
        if (e.isActive() != desiredActive) {
            e.setActive(desiredActive);
            eventRepository.save(e);
        }
    }

    public void signup(String eventId, VolunteerSignupRequest request) {
        User user = SecurityUtils.requireCurrentUser();
        VolunteerEvent event =
                eventRepository
                        .findById(eventId)
                        .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        syncActiveFlagForEvent(event);
        if (!event.isActive()) {
            throw new BadRequestException("Event is not active");
        }
        if (!event.getEventDate().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Signups are closed for this event");
        }
        if (signupRepository.existsByEventIdAndUserId(eventId, user.getId())) {
            throw new BadRequestException("Already signed up for this event");
        }
        VolunteerSignup signup =
                VolunteerSignup.builder()
                        .eventId(eventId)
                        .userId(user.getId())
                        .participantName(request.getParticipantName().trim())
                        .mobileNumber(request.getMobileNumber().trim())
                        .signedUpAt(Instant.now())
                        .build();
        signupRepository.save(signup);
        List<VolunteerEventResponse> eventsSnap = listEventsForPortal();
        VolunteerSignupAdminResponse signupsSnap = listSignupsForAdmin(eventId);
        liveUpdatePublisher.runAfterCommit(
                () -> {
                    liveUpdatePublisher.publishVolunteerEvents(eventsSnap);
                    liveUpdatePublisher.publishAdminVolunteerSignups(
                            VolunteerSignupsPush.builder()
                                    .eventId(eventId)
                                    .payload(signupsSnap)
                                    .build());
                });
    }

    public VolunteerEventResponse createEvent(CreateVolunteerEventRequest request) {
        LocalDateTime now = LocalDateTime.now();
        // Active only while the scheduled instant is still in the future (matches signup rules).
        boolean active = request.getEventDate().isAfter(now);
        VolunteerEvent event =
                VolunteerEvent.builder()
                        .title(request.getTitle())
                        .description(request.getDescription())
                        .volunteersRequired(request.getVolunteersRequired())
                        .eventDate(request.getEventDate())
                        .active(active)
                        .build();
        eventRepository.save(event);
        VolunteerEventResponse created = toEventResponse(event);
        List<VolunteerEventResponse> eventsSnap = listEventsForPortal();
        liveUpdatePublisher.runAfterCommit(() -> liveUpdatePublisher.publishVolunteerEvents(eventsSnap));
        return created;
    }

    public VolunteerSignupAdminResponse listSignupsForAdmin(String eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event not found");
        }
        long count = signupRepository.countByEventId(eventId);
        List<VolunteerSignupAdminResponse.SignupRow> rows =
                signupRepository.findByEventIdOrderBySignedUpAtAsc(eventId).stream()
                        .map(
                                s -> {
                                    User u =
                                            userRepository
                                                    .findById(s.getUserId())
                                                    .orElseThrow(
                                                            () ->
                                                                    new ResourceNotFoundException(
                                                                            "User not found"));
                                    String displayName =
                                            s.getParticipantName() != null && !s.getParticipantName().isBlank()
                                                    ? s.getParticipantName()
                                                    : u.getFullName();
                                    return VolunteerSignupAdminResponse.SignupRow.builder()
                                            .participantName(displayName)
                                            .mobileNumber(s.getMobileNumber())
                                            .signedUpAt(s.getSignedUpAt())
                                            .build();
                                })
                        .toList();
        return VolunteerSignupAdminResponse.builder().count(count).signups(rows).build();
    }

    private VolunteerEventResponse toEventResponse(VolunteerEvent e) {
        LocalDateTime now = LocalDateTime.now();
        boolean schedulePast = e.getEventDate().isBefore(now);
        // List "Past" by actual schedule, not by stored active (inactive future events stay upcoming in the UI).
        boolean completed = schedulePast;
        boolean signupOpen = e.isActive() && e.getEventDate().isAfter(now);
        long c = signupRepository.countByEventId(e.getId());
        boolean signedUp =
                SecurityUtils.optionalCurrentUser()
                        .map(u -> signupRepository.existsByEventIdAndUserId(e.getId(), u.getId()))
                        .orElse(false);
        return VolunteerEventResponse.builder()
                .id(e.getId())
                .title(e.getTitle())
                .description(e.getDescription())
                .volunteersRequired(e.getVolunteersRequired())
                .eventDate(e.getEventDate())
                .active(e.isActive())
                .currentSignups(c)
                .currentUserSignedUp(signedUp)
                .completed(completed)
                .schedulePast(schedulePast)
                .signupOpen(signupOpen)
                .build();
    }
}
