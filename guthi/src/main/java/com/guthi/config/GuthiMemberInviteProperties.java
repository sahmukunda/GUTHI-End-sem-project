package com.guthi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * When enabled, {@code POST /api/auth/register-guthi-member} accepts new accounts with role
 * ADMIN if {@link #code} matches. Keep disabled in production unless you intentionally onboard
 * trustees via a shared secret or env-supplied code.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "guthi.member-invite")
public class GuthiMemberInviteProperties {

    private boolean enabled = false;

    /** Invite code compared using {@link String#equals} (trimmed on request). */
    private String code = "";
}
