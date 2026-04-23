package com.guthi.dto.response;

import com.guthi.enums.Role;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthResponse {
    String token;
    String email;
    String fullName;
    Role role;
}
