package com.guthi.controller;

import com.guthi.dto.request.AdminDecisionRequest;
import com.guthi.dto.request.CreateCommunityRequestDto;
import com.guthi.dsa.GuthiLands;
import com.guthi.dto.response.CommunityRequestResponse;
import com.guthi.dto.response.VolunteerCallResponse;
import com.guthi.model.User;
import com.guthi.security.SecurityUtils;
import com.guthi.service.RequestService;
import com.guthi.service.RequestVolunteerService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;
    private final RequestVolunteerService volunteerService;

    @PostMapping
    public ResponseEntity<CommunityRequestResponse> create(
            @Valid @RequestBody CreateCommunityRequestDto body) {
        return ResponseEntity.ok(requestService.create(body));
    }

    @GetMapping("/lands")
    public ResponseEntity<List<String>> lands() {
        return ResponseEntity.ok(GuthiLands.NAMES);
    }

    @GetMapping("/my")
    public ResponseEntity<List<CommunityRequestResponse>> myRequests() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(requestService.myRequests());
    }

    @GetMapping("/volunteer-calls")
    public ResponseEntity<List<VolunteerCallResponse>> myVolunteerCalls() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(volunteerService.myCalls());
    }

    @PutMapping("/volunteer-calls/{id}/accept")
    public ResponseEntity<List<VolunteerCallResponse>> acceptVolunteerCall(@PathVariable String id) {
        List<String> affected = volunteerService.accept(id);
        livePublish(affected);
        return ResponseEntity.ok(volunteerService.myCalls());
    }

    @PutMapping("/volunteer-calls/{id}/decline")
    public ResponseEntity<List<VolunteerCallResponse>> declineVolunteerCall(@PathVariable String id) {
        List<String> affected = volunteerService.decline(id);
        livePublish(affected);
        return ResponseEntity.ok(volunteerService.myCalls());
    }

    private void livePublish(List<String> userIds) {
        volunteerService.publishForUsers(userIds);
    }

    /** Admin-only list; path must stay distinct from {@code GET /my} (member list). */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CommunityRequestResponse>> listAll() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(requestService.listAllForAdmin());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommunityRequestResponse> update(
            @PathVariable String id, @Valid @RequestBody CreateCommunityRequestDto body) {
        return ResponseEntity.ok(requestService.update(id, body));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        requestService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommunityRequestResponse> approve(
            @PathVariable String id, @Valid @RequestBody(required = false) AdminDecisionRequest body) {
        User admin = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(requestService.approve(id, body, admin));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommunityRequestResponse> reject(
            @PathVariable String id, @Valid @RequestBody(required = false) AdminDecisionRequest body) {
        return ResponseEntity.ok(requestService.reject(id, body));
    }
}
