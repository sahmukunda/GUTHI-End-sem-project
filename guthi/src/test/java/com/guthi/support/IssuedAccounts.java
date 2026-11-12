package com.guthi.support;

import com.guthi.enums.Role;
import com.guthi.model.User;
import com.guthi.repository.UserRepository;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Test stand-in for a technician inserting an account directly into {@code users}. */
public final class IssuedAccounts {

    private IssuedAccounts() {}

    public static void issue(
            UserRepository users,
            PasswordEncoder encoder,
            String fullName,
            String email,
            String rawPassword,
            Role role) {
        if (users.existsByEmailIgnoreCase(email)) {
            return;
        }
        users.save(
                User.builder()
                        .fullName(fullName)
                        .email(email.trim().toLowerCase())
                        .password(encoder.encode(rawPassword))
                        .mobileNumber("9800000000")
                        .role(role)
                        .createdAt(Instant.now())
                        .build());
    }
}
