package com.guthi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "guthi.jwt")
public class JwtConfig {

    /** HS256 key material (use a long random secret in production). */
    private String secret = "change-me";

    /** Token TTL in milliseconds. */
    private long expiration = 86400000L;
}
