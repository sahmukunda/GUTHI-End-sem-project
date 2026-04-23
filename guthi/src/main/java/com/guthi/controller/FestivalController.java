package com.guthi.controller;

import com.guthi.dto.request.CreateFestivalRequest;
import com.guthi.dto.response.FestivalResponse;
import com.guthi.service.FestivalService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/festivals")
@RequiredArgsConstructor
public class FestivalController {

    private final FestivalService festivalService;

    @GetMapping
    public ResponseEntity<List<FestivalResponse>> list() {
        return ResponseEntity.ok(festivalService.listFestivals());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FestivalResponse> create(@Valid @RequestBody CreateFestivalRequest body) {
        return ResponseEntity.ok(festivalService.create(body));
    }
}
