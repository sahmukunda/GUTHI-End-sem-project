package com.guthi.service;

import com.guthi.config.GuthiMemberInviteProperties;
import com.guthi.config.JwtConfig;
import com.guthi.dto.request.LoginRequest;
import com.guthi.dto.request.RegisterGuthiMemberRequest;
import com.guthi.dto.request.RegisterRequest;
import com.guthi.dto.response.AuthResponse;
import com.guthi.enums.Role;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final GuthiMemberInviteProperties memberInviteProperties;
    private final AuthSessionService authSessionService;
    private final JwtConfig jwtConfig;

    @PostConstruct
    void logMemberInviteConfig() {
        log.info(
                "Guthi member self-registration: enabled={}, invite code configured={}",
                memberInviteProperties.isEnabled(),
                memberInviteProperties.getCode() != null && !memberInviteProperties.getCode().isBlank());
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }
        User user =
                User.builder()
                        .fullName(request.getFullName())
                        .email(request.getEmail().trim().toLowerCase())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .mobileNumber(request.getMobileNumber())
                        .role(Role.USER)
                        .createdAt(Instant.now())
                        .build();
        userRepository.save(user);
        return issueAuthResponse(user);
    }

    public AuthResponse registerGuthiMember(RegisterGuthiMemberRequest request) {
        if (!memberInviteProperties.isEnabled()) {
            throw new BadRequestException("Guthi member self-registration is not enabled");
        }
        String expected = memberInviteProperties.getCode() == null ? "" : memberInviteProperties.getCode().trim();
        if (expected.isEmpty() || !expected.equals(request.getInviteCode().trim())) {
            throw new BadRequestException("Invalid invite code");
        }
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }
        User user =
                User.builder()
                        .fullName(request.getFullName())
                        .email(request.getEmail().trim().toLowerCase())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .mobileNumber(request.getMobileNumber())
                        .role(Role.ADMIN)
                        .createdAt(Instant.now())
                        .build();
        userRepository.save(user);
        return issueAuthResponse(user);
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
