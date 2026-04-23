package com.guthi.controller;

import com.guthi.dto.request.CreateVolunteerEventRequest;
import com.guthi.dto.request.VolunteerSignupRequest;
import com.guthi.dto.response.VolunteerEventResponse;
import com.guthi.dto.response.VolunteerSignupAdminResponse;
import com.guthi.service.VolunteerService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/volunteer-events")
@RequiredArgsConstructor
public class VolunteerController {

    private final VolunteerService volunteerService;

    @GetMapping
    public ResponseEntity<List<VolunteerEventResponse>> list() {
        return ResponseEntity.ok(volunteerService.listEventsForPortal());
    }

    @PostMapping("/{id}/signup")
    public ResponseEntity<Void> signup(
            @PathVariable String id, @Valid @RequestBody VolunteerSignupRequest body) {
        volunteerService.signup(id, body);
        return ResponseEntity.ok().build();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VolunteerEventResponse> create(
            @Valid @RequestBody CreateVolunteerEventRequest body) {
        return ResponseEntity.ok(volunteerService.createEvent(body));
    }

    @GetMapping("/{id}/signups")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VolunteerSignupAdminResponse> signups(@PathVariable String id) {
        return ResponseEntity.ok(volunteerService.listSignupsForAdmin(id));
    }
}
