package com.guthi.service;

import com.guthi.config.JwtConfig;
import com.guthi.dto.request.LoginRequest;
import com.guthi.dto.request.RegisterGuthiMemberRequest;
import com.guthi.dto.request.RegisterRequest;
import com.guthi.dto.response.AuthResponse;
import com.guthi.exception.BadRequestException;
import com.guthi.model.User;
import com.guthi.repository.UserRepository;
import com.guthi.security.AuthSessionService;
import com.guthi.security.JwtService;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuthSessionService authSessionService;
    private final JwtConfig jwtConfig;

    private static final String REGISTRATION_CLOSED =
            "Self-registration is closed. A technician must add this account in the database before you can sign in.";

    @PostConstruct
    void logRegistrationClosed() {
        log.info("Account self-registration is closed. Sign-in only, for accounts already stored in users.");
    }

    public AuthResponse register(RegisterRequest request) {
        throw new BadRequestException(REGISTRATION_CLOSED);
    }

    public AuthResponse registerGuthiMember(RegisterGuthiMemberRequest request) {
        throw new BadRequestException(REGISTRATION_CLOSED);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().trim().toLowerCase(), request.getPassword()));
        User user =
                userRepository
                        .findByEmailIgnoreCase(request.getEmail().trim().toLowerCase())
                        .orElseThrow(() -> new BadRequestException("Invalid credentials"));
        return issueAuthResponse(user);
    }

    public void logout(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) {
            return;
        }
        try {
            String jti = jwtService.extractJti(bearerToken);
            authSessionService.revoke(jti);
        } catch (Exception ignored) {
            // Invalid/expired token → nothing to revoke.
        }
    }

    private AuthResponse issueAuthResponse(User user) {
        Instant expiresAt = Instant.now().plusMillis(jwtConfig.getExpiration());
        String jti = authSessionService.createSession(user.getEmail(), expiresAt, null);
        String token = jwtService.generateToken(user, jti);
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }
}
