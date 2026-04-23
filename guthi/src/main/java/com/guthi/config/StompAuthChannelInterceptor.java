package com.guthi.config;

import com.guthi.model.User;
import com.guthi.security.JwtService;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            User fromHandshake = null;
            if (accessor.getSessionAttributes() != null) {
                Object u = accessor.getSessionAttributes().get(JwtWebSocketHandshakeInterceptor.SESSION_USER);
                if (u instanceof User user) {
                    fromHandshake = user;
                }
            }
            if (fromHandshake != null) {
                setUser(accessor, fromHandshake);
                return message;
            }
            List<String> authHeaders = accessor.getNativeHeader("Authorization");
            if (authHeaders != null && !authHeaders.isEmpty()) {
                String raw = authHeaders.get(0);
                if (raw != null && raw.startsWith("Bearer ")) {
                    String token = raw.substring(7).trim();
                    try {
                        String email = jwtService.extractUsername(token);
                        UserDetails details = userDetailsService.loadUserByUsername(email);
                        if (jwtService.isTokenValid(token, details) && details instanceof User user) {
                            setUser(accessor, user);
                        }
                    } catch (Exception ignored) {
                        // anonymous connect
                    }
                }
            }
            return message;
        }
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String dest = accessor.getDestination();
            if (dest == null) {
                return message;
            }
            if (dest.startsWith("/topic/admin/")) {
                requireNonAnonymous(accessor);
                requireAdmin(accessor);
            } else if (dest.startsWith("/user/queue/") || dest.startsWith("/user/queue")) {
                requireNonAnonymous(accessor);
            }
        }
        return message;
    }

    private static void setUser(StompHeaderAccessor accessor, User user) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities());
        accessor.setUser(auth);
    }

    private static void requireNonAnonymous(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (user == null) {
            throw new IllegalStateException("Subscription requires authentication");
        }
    }

    private static void requireAdmin(StompHeaderAccessor accessor) {
        Principal p = accessor.getUser();
        if (!(p instanceof Authentication auth)) {
            throw new IllegalStateException("Subscription requires authentication");
        }
        boolean admin =
                auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(a -> "ROLE_ADMIN".equals(a));
        if (!admin) {
            throw new IllegalStateException("Admin role required for this destination");
        }
    }
}
