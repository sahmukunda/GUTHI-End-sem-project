package com.guthi.config;

import com.guthi.model.User;
import com.guthi.repository.UserRepository;
import com.guthi.security.AuthSessionService;
import com.guthi.security.JwtService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
@RequiredArgsConstructor
public class JwtWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    public static final String SESSION_USER = "GUTHI_USER";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final AuthSessionService authSessionService;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return true;
        }
        String token = servletRequest.getServletRequest().getParameter("access_token");
        if (token == null || token.isBlank()) {
            return true;
        }
        try {
            String email = jwtService.extractUsername(token);
            String jti = jwtService.extractJti(token);
            UserDetails details = userDetailsService.loadUserByUsername(email);
            if (!jwtService.isTokenValid(token, details) || !authSessionService.isActive(jti)) {
                return true;
            }
            User user =
                    userRepository
                            .findByEmailIgnoreCase(email)
                            .orElse(null);
            if (user != null) {
                attributes.put(SESSION_USER, user);
            }
        } catch (Exception ignored) {
            // Treat as anonymous
        }
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // no-op
    }
}
