package com.guthi.controller.admin;

import com.guthi.model.User;
import com.guthi.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    @GetMapping("/me")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminMeResponse> me() {
        User u = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(
                new AdminMeResponse(u.getFullName(), u.getEmail(), u.getRole().name()));
    }

    public record AdminMeResponse(String fullName, String email, String role) {}
}
