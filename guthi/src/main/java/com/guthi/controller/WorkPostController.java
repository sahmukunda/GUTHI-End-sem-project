package com.guthi.controller;

import com.guthi.dto.request.CreateWorkPostRequest;
import com.guthi.dto.response.WorkPostResponse;
import com.guthi.service.WorkPostService;
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
@RequestMapping("/api/work-posts")
@RequiredArgsConstructor
public class WorkPostController {

    private final WorkPostService workPostService;

    @PostMapping
    public ResponseEntity<WorkPostResponse> create(@Valid @RequestBody CreateWorkPostRequest body) {
        return ResponseEntity.ok(workPostService.create(body));
    }

    @GetMapping
    public ResponseEntity<List<WorkPostResponse>> list() {
        return ResponseEntity.ok(workPostService.listAll());
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<WorkPostResponse> accept(@PathVariable String id) {
        return ResponseEntity.ok(workPostService.accept(id));
    }
}
