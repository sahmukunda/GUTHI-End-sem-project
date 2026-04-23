package com.guthi.controller.admin;

import com.guthi.dto.response.FestivalResponse;
import com.guthi.service.FestivalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only festival actions. Lives under {@code /api/admin/**} so routing matches other admin
 * APIs and {@link org.springframework.security.config.annotation.web.builders.HttpSecurity} rules.
 */
@RestController
@RequestMapping("/api/admin/festivals")
@RequiredArgsConstructor
public class AdminFestivalController {

    private final FestivalService festivalService;

    @RequestMapping(value = "/{id}/complete", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FestivalResponse> markCompleted(@PathVariable String id) {
        return ResponseEntity.ok(festivalService.markCompleted(id));
    }
}
